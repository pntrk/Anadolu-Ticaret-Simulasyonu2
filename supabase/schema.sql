-- =============================================================================
-- SUPABASE POSTGRESQL DATABASE SCHEMA: ANADOLU TICARET SIMULASYONU
-- Multi-Tier Item Quality & Crafting Architecture (quality_level 1-5)
-- =============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -----------------------------------------------------------------------------
-- 1. ENUM & QUALITY MULTIPLIER HELPER FUNCTIONS
-- -----------------------------------------------------------------------------

-- Returns price multiplier for given quality level (1: 1.0x, 2: 1.35x, 3: 1.90x, 4: 2.80x, 5: 4.50x)
CREATE OR REPLACE FUNCTION get_quality_price_multiplier(p_quality_level integer)
RETURNS numeric AS $$
BEGIN
    RETURN CASE p_quality_level
        WHEN 1 THEN 1.00
        WHEN 2 THEN 1.35
        WHEN 3 THEN 1.90
        WHEN 4 THEN 2.80
        WHEN 5 THEN 4.50
        ELSE 1.00
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Returns label for given quality level
CREATE OR REPLACE FUNCTION get_quality_label(p_quality_level integer)
RETURNS text AS $$
BEGIN
    RETURN CASE p_quality_level
        WHEN 1 THEN '⭐ Standart'
        WHEN 2 THEN '⭐⭐ Seçme'
        WHEN 3 THEN '⭐⭐⭐ Usta İşi'
        WHEN 4 THEN '⭐⭐⭐⭐ Seçkin'
        WHEN 5 THEN '⭐⭐⭐⭐⭐ Kusursuz'
        ELSE '⭐ Standart'
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Calculates crafted product quality level based on formula:
-- Inputs: (Avg Input Quality * 0.45) + (Facility Level * 0.40) + (Critical Roll [0 or 1] * 0.15)
-- Direct Harvest: (Facility Level * 0.80) + (Critical Roll [0 or 1] * 0.20)
CREATE OR REPLACE FUNCTION calculate_crafting_quality(
    p_avg_input_quality numeric,
    p_facility_level integer,
    p_critical_craft boolean,
    p_has_inputs boolean
) RETURNS integer AS $$
DECLARE
    v_crit_val numeric := CASE WHEN p_critical_craft THEN 1.0 ELSE 0.0 END;
    v_fac_lvl numeric := LEAST(GREATEST(p_facility_level::numeric, 1.0), 5.0);
    v_raw_score numeric;
    v_final_tier integer;
BEGIN
    IF p_has_inputs THEN
        v_raw_score := (COALESCE(p_avg_input_quality, 1.0) * 0.45) + (v_fac_lvl * 0.40) + (v_crit_val * 0.15);
    ELSE
        v_raw_score := (v_fac_lvl * 0.80) + (v_crit_val * 0.20);
    END IF;
    
    v_final_tier := ROUND(v_raw_score);
    RETURN LEAST(GREATEST(v_final_tier, 1), 5);
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- -----------------------------------------------------------------------------
-- 2. PLAYERS TABLE (Cloud Save & Profile)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.players (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL DEFAULT 'Tüccar',
    company_name TEXT NOT NULL DEFAULT 'Tüccar Holding',
    money BIGINT NOT NULL DEFAULT 100000,
    loan_amount BIGINT NOT NULL DEFAULT 0,
    deposit_balance BIGINT NOT NULL DEFAULT 0,
    daily_income BIGINT NOT NULL DEFAULT 0,
    daily_expense BIGINT NOT NULL DEFAULT 0,
    total_profit BIGINT NOT NULL DEFAULT 0,
    xp INTEGER NOT NULL DEFAULT 0,
    level INTEGER NOT NULL DEFAULT 1,
    inventory_capacity INTEGER NOT NULL DEFAULT 5000,
    current_city TEXT NOT NULL DEFAULT 'istanbul',
    is_vip BOOLEAN NOT NULL DEFAULT FALSE,
    gems INTEGER NOT NULL DEFAULT 0,
    last_daily_reward_ms BIGINT NOT NULL DEFAULT 0,
    login_streak INTEGER NOT NULL DEFAULT 0,
    dollar_balance BIGINT NOT NULL DEFAULT 0,
    dollar_deposit_balance BIGINT NOT NULL DEFAULT 0,
    dollar_loan_amount BIGINT NOT NULL DEFAULT 0,
    is_online_registered BOOLEAN NOT NULL DEFAULT TRUE,
    online_email TEXT DEFAULT '',
    businesses JSONB NOT NULL DEFAULT '[]'::jsonb,
    inventory JSONB NOT NULL DEFAULT '[]'::jsonb,
    active_deliveries JSONB NOT NULL DEFAULT '[]'::jsonb,
    active_productions JSONB NOT NULL DEFAULT '[]'::jsonb,
    managers JSONB NOT NULL DEFAULT '[]'::jsonb,
    daily_quest_state JSONB NOT NULL DEFAULT '{}'::jsonb,
    active_researches JSONB NOT NULL DEFAULT '[]'::jsonb,
    research_levels JSONB NOT NULL DEFAULT '{}'::jsonb,
    guild_shares JSONB NOT NULL DEFAULT '{}'::jsonb,
    guild_buy_prices JSONB NOT NULL DEFAULT '{}'::jsonb,
    raw_save_json JSONB,
    last_saved_time BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Idempotent column additions for existing installations
ALTER TABLE IF EXISTS public.players
    ADD COLUMN IF NOT EXISTS dollar_balance BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS dollar_deposit_balance BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS dollar_loan_amount BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_online_registered BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS online_email TEXT DEFAULT '',
    ADD COLUMN IF NOT EXISTS businesses JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS inventory JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS active_deliveries JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS active_productions JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS managers JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS daily_quest_state JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS active_researches JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS research_levels JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS guild_shares JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS guild_buy_prices JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS raw_save_json JSONB,
    ADD COLUMN IF NOT EXISTS last_saved_time BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_players_money ON public.players(money DESC);
CREATE INDEX IF NOT EXISTS idx_players_level ON public.players(level DESC);

-- -----------------------------------------------------------------------------
-- 3. PRODUCTS & MARKET PRICES (With quality_level)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.market_prices (
    id TEXT PRIMARY KEY, -- Format: 'productId_qualityLevel' e.g. 'iron_1', 'steel_5'
    item_id TEXT NOT NULL,
    quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    quality_label TEXT GENERATED ALWAYS AS (get_quality_label(quality_level)) STORED,
    item_name TEXT NOT NULL,
    base_price BIGINT NOT NULL,
    price_multiplier NUMERIC(4, 2) GENERATED ALWAYS AS (get_quality_price_multiplier(quality_level)) STORED,
    current_price BIGINT NOT NULL,
    calculated_price BIGINT GENERATED ALWAYS AS (ROUND(base_price * get_quality_price_multiplier(quality_level))) STORED,
    change_rate NUMERIC(5, 2) NOT NULL DEFAULT 0.0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_market_prices_item ON public.market_prices(item_id, quality_level);

-- -----------------------------------------------------------------------------
-- 4. GLOBAL MARKET LISTINGS (Peer-to-Peer Market with quality_level)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.global_market (
    id TEXT PRIMARY KEY,
    seller_id TEXT NOT NULL,
    seller_name TEXT NOT NULL,
    item_id TEXT NOT NULL,
    quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    quality_tier TEXT NOT NULL DEFAULT 'Standart',
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price_per_unit BIGINT NOT NULL CHECK (price_per_unit > 0),
    total_price BIGINT GENERATED ALWAYS AS (quantity * price_per_unit) STORED,
    city TEXT NOT NULL DEFAULT 'istanbul',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_global_market_item_quality ON public.global_market(item_id, quality_level);
CREATE INDEX IF NOT EXISTS idx_global_market_seller ON public.global_market(seller_id);
CREATE INDEX IF NOT EXISTS idx_global_market_created ON public.global_market(created_at DESC);

-- -----------------------------------------------------------------------------
-- 5. BUY ORDERS (Procurement Demands with quality_level)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.buy_orders (
    id TEXT PRIMARY KEY,
    buyer_id TEXT NOT NULL,
    buyer_name TEXT NOT NULL,
    item_id TEXT NOT NULL,
    quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    min_quality_level INTEGER NOT NULL DEFAULT 1 CHECK (min_quality_level BETWEEN 1 AND 5),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price_per_unit BIGINT NOT NULL CHECK (price_per_unit > 0),
    total_budget BIGINT GENERATED ALWAYS AS (quantity * price_per_unit) STORED,
    destination_city_id TEXT NOT NULL DEFAULT 'istanbul',
    is_fulfilled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_buy_orders_item_quality ON public.buy_orders(item_id, quality_level);
CREATE INDEX IF NOT EXISTS idx_buy_orders_buyer ON public.buy_orders(buyer_id);

-- -----------------------------------------------------------------------------
-- 6. FUTURES CONTRACTS (Vadeli İşlem Sözleşmeleri with quality_level)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.futures_contracts (
    id TEXT PRIMARY KEY,
    creator_id TEXT NOT NULL,
    creator_name TEXT NOT NULL,
    item_id TEXT NOT NULL,
    quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    locked_price_per_unit BIGINT NOT NULL CHECK (locked_price_per_unit > 0),
    total_locked_value BIGINT GENERATED ALWAYS AS (quantity * locked_price_per_unit) STORED,
    duration_days INTEGER NOT NULL DEFAULT 30,
    city_id TEXT NOT NULL DEFAULT 'istanbul',
    is_fulfilled BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '30 days'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_futures_item_quality ON public.futures_contracts(item_id, quality_level);
CREATE INDEX IF NOT EXISTS idx_futures_creator ON public.futures_contracts(creator_id);

-- -----------------------------------------------------------------------------
-- 7. TRADE & PRODUCT TRANSACTIONS LOG (Full Audit & Backup History)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.trade_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_type TEXT NOT NULL, -- 'MARKET_BUY', 'MARKET_SELL', 'FUTURES_SETTLE', 'CONSORTIUM_SUPPLY', 'DIRECT_TRADE'
    buyer_id TEXT,
    buyer_name TEXT,
    seller_id TEXT,
    seller_name TEXT,
    item_id TEXT NOT NULL,
    quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price BIGINT NOT NULL CHECK (unit_price >= 0),
    total_amount BIGINT NOT NULL CHECK (total_amount >= 0),
    city_id TEXT NOT NULL DEFAULT 'istanbul',
    synergy_bonus_percent INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trade_tx_item_quality ON public.trade_transactions(item_id, quality_level);
CREATE INDEX IF NOT EXISTS idx_trade_tx_buyer ON public.trade_transactions(buyer_id);
CREATE INDEX IF NOT EXISTS idx_trade_tx_seller ON public.trade_transactions(seller_id);
CREATE INDEX IF NOT EXISTS idx_trade_tx_created ON public.trade_transactions(created_at DESC);

-- -----------------------------------------------------------------------------
-- 8. CONSORTIUM MEGA PROJECTS & SLOTS (With craftsmanship & quality_level)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.global_guilds (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    leader_name TEXT NOT NULL,
    member_count INTEGER NOT NULL DEFAULT 1,
    mega_project_title TEXT NOT NULL,
    mega_project_target BIGINT NOT NULL DEFAULT 0,
    mega_project_current BIGINT NOT NULL DEFAULT 0,
    mega_project_requirements JSONB NOT NULL DEFAULT '{}'::jsonb,
    mega_project_contributions JSONB NOT NULL DEFAULT '{}'::jsonb,
    target_quality_grade TEXT NOT NULL DEFAULT 'C' CHECK (target_quality_grade IN ('A', 'B', 'C', 'GRADE_A', 'GRADE_B', 'GRADE_C')),
    quality_tier TEXT NOT NULL DEFAULT 'GRADE_C',
    allowed_input_qualities TEXT NOT NULL DEFAULT '[1,2]', -- A: [4,5], B: [2,3,4], C: [1,2]
    slot_quality_levels JSONB NOT NULL DEFAULT '{}'::jsonb, -- e.g. {"cement": 4, "steel": 5}
    average_craftsmanship_score NUMERIC(4, 2) NOT NULL DEFAULT 1.0,
    master_craftsmanship_tier INTEGER NOT NULL DEFAULT 1 CHECK (master_craftsmanship_tier BETWEEN 1 AND 5),
    craftsmanship_multiplier NUMERIC(4, 2) NOT NULL DEFAULT 1.0,
    perk_description TEXT DEFAULT '',
    bank_balance BIGINT NOT NULL DEFAULT 0,
    is_ipo_active BOOLEAN NOT NULL DEFAULT FALSE,
    public_share_percent INTEGER NOT NULL DEFAULT 20,
    target_product_name TEXT DEFAULT '',
    target_product_id TEXT DEFAULT '',
    warehouse_stock INTEGER NOT NULL DEFAULT 0,
    total_items_produced INTEGER NOT NULL DEFAULT 0,
    unit_batch_price BIGINT NOT NULL DEFAULT 0,
    current_stage TEXT DEFAULT '',
    raw_project_json JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 9. MUSEUM ARTIFACTS & AUCTIONS (With quality_level / masterwork)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.museum_artifacts (
    artifact_id TEXT PRIMARY KEY,
    owner_id TEXT,
    owner_name TEXT NOT NULL DEFAULT 'T.C. Kültür ve Turizm Bakanlığı',
    quality_level INTEGER NOT NULL DEFAULT 5 CHECK (quality_level BETWEEN 1 AND 5),
    status TEXT NOT NULL DEFAULT 'UNCLAIMED_TREASURY',
    active_auction_id TEXT,
    last_price BIGINT NOT NULL DEFAULT 0,
    updated_at_ms BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.museum_auctions (
    id TEXT PRIMARY KEY,
    artifact_id TEXT NOT NULL REFERENCES public.museum_artifacts(artifact_id) ON DELETE CASCADE,
    seller_id TEXT NOT NULL,
    seller_name TEXT NOT NULL,
    quality_level INTEGER NOT NULL DEFAULT 5 CHECK (quality_level BETWEEN 1 AND 5),
    is_player_seller BOOLEAN NOT NULL DEFAULT TRUE,
    starting_bid BIGINT NOT NULL,
    current_highest_bid BIGINT NOT NULL,
    current_highest_bidder_id TEXT DEFAULT '',
    current_highest_bidder_name TEXT DEFAULT '',
    buyout_price BIGINT NOT NULL DEFAULT 0,
    ends_at_ms BIGINT NOT NULL,
    bid_count INTEGER NOT NULL DEFAULT 0,
    is_settled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 10. ROW LEVEL SECURITY (RLS) POLICIES
-- -----------------------------------------------------------------------------
ALTER TABLE public.players ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.market_prices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.global_market ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.buy_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.futures_contracts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trade_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.global_guilds ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.museum_artifacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.museum_auctions ENABLE ROW LEVEL SECURITY;

-- Anonymous and authenticated read-write for simulation state
DROP POLICY IF EXISTS "Public read players" ON public.players;
CREATE POLICY "Public read players" ON public.players FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public update players" ON public.players;
CREATE POLICY "Public update players" ON public.players FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Public read market_prices" ON public.market_prices;
CREATE POLICY "Public read market_prices" ON public.market_prices FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public update market_prices" ON public.market_prices;
CREATE POLICY "Public update market_prices" ON public.market_prices FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read global_market" ON public.global_market;
CREATE POLICY "Public read global_market" ON public.global_market FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public modify global_market" ON public.global_market;
CREATE POLICY "Public modify global_market" ON public.global_market FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read buy_orders" ON public.buy_orders;
CREATE POLICY "Public read buy_orders" ON public.buy_orders FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public modify buy_orders" ON public.buy_orders;
CREATE POLICY "Public modify buy_orders" ON public.buy_orders FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read futures" ON public.futures_contracts;
CREATE POLICY "Public read futures" ON public.futures_contracts FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public modify futures" ON public.futures_contracts;
CREATE POLICY "Public modify futures" ON public.futures_contracts FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read trade_tx" ON public.trade_transactions;
CREATE POLICY "Public read trade_tx" ON public.trade_transactions FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public insert trade_tx" ON public.trade_transactions;
CREATE POLICY "Public insert trade_tx" ON public.trade_transactions FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read guilds" ON public.global_guilds;
CREATE POLICY "Public read guilds" ON public.global_guilds FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public modify guilds" ON public.global_guilds;
CREATE POLICY "Public modify guilds" ON public.global_guilds FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read artifacts" ON public.museum_artifacts;
CREATE POLICY "Public read artifacts" ON public.museum_artifacts FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public modify artifacts" ON public.museum_artifacts;
CREATE POLICY "Public modify artifacts" ON public.museum_artifacts FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read auctions" ON public.museum_auctions;
CREATE POLICY "Public read auctions" ON public.museum_auctions FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public modify auctions" ON public.museum_auctions;
CREATE POLICY "Public modify auctions" ON public.museum_auctions FOR ALL USING (true);

-- -----------------------------------------------------------------------------
-- 11. PLAYER META TABLE (Lightweight Leaderboard & Anti-Cheat Sync)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.player_meta (
    player_id TEXT PRIMARY KEY,
    name TEXT NOT NULL DEFAULT 'Tüccar',
    level INT NOT NULL DEFAULT 1,
    net_worth BIGINT NOT NULL DEFAULT 0,
    anti_cheat_hash TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE IF EXISTS public.market_prices
    ADD COLUMN IF NOT EXISTS stock BIGINT NOT NULL DEFAULT 999999,
    ADD COLUMN IF NOT EXISTS borsa_stock BIGINT NOT NULL DEFAULT 999999;

CREATE INDEX IF NOT EXISTS idx_player_meta_net_worth ON public.player_meta(net_worth DESC);
CREATE INDEX IF NOT EXISTS idx_player_meta_level ON public.player_meta(level DESC);

ALTER TABLE public.player_meta ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public read player_meta" ON public.player_meta;
CREATE POLICY "Public read player_meta" ON public.player_meta FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public upsert player_meta" ON public.player_meta;
CREATE POLICY "Public upsert player_meta" ON public.player_meta FOR ALL USING (true) WITH CHECK (true);

-- -----------------------------------------------------------------------------
-- 12. RPC FUNCTION: EXECUTE_BORSA_BUY
-- PostgreSQL FOR UPDATE lock on market_prices to prevent race conditions and stock conflicts
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION execute_borsa_buy(
    p_player_id TEXT,
    p_item_id TEXT,
    p_quantity INT,
    p_max_acceptable_price BIGINT DEFAULT 0
) RETURNS jsonb AS $$
DECLARE
    v_rec RECORD;
    v_stock BIGINT;
    v_base_price BIGINT;
    v_current_price BIGINT;
    v_unit_price BIGINT;
    v_total_cost BIGINT;
    v_new_stock BIGINT;
    v_new_price BIGINT;
    v_s0 NUMERIC := 999999.0;
    v_safe_stock NUMERIC;
    v_computed_price NUMERIC;
    v_raw_base NUMERIC;
    v_stock_drop_ratio NUMERIC;
    v_price_increase_factor NUMERIC;
    v_min_price BIGINT;
    v_max_price BIGINT;
    v_player_money BIGINT;
BEGIN
    IF p_quantity <= 0 THEN
        RETURN jsonb_build_object('success', false, 'error', 'INVALID_QUANTITY', 'message', 'Geçersiz miktar');
    END IF;

    -- 1. Lock the market price row for the item using SELECT ... FOR UPDATE
    SELECT * INTO v_rec 
    FROM public.market_prices 
    WHERE item_id = p_item_id OR id = p_item_id
    LIMIT 1
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'ITEM_NOT_FOUND', 'message', 'Ürün borsa sisteminde bulunamadı');
    END IF;

    -- 2. Extract current stock and price
    v_stock := COALESCE(v_rec.borsa_stock, v_rec.stock, 999999);
    v_base_price := GREATEST(COALESCE(v_rec.base_price, 10), 10);
    v_current_price := GREATEST(COALESCE(v_rec.current_price, v_base_price), 1);

    -- Check if stock is sufficient
    IF v_stock < p_quantity THEN
        RETURN jsonb_build_object(
            'success', false, 
            'error', 'INSUFFICIENT_STOCK', 
            'message', 'Borsada yeterli stok kalmadı',
            'available_stock', v_stock
        );
    END IF;

    -- 3. Calculate unit price & total cost
    v_unit_price := v_current_price;
    v_total_cost := v_unit_price * p_quantity;

    -- Check slippage / max acceptable price
    IF p_max_acceptable_price > 0 AND v_unit_price > p_max_acceptable_price THEN
        RETURN jsonb_build_object(
            'success', false, 
            'error', 'PRICE_EXCEEDED', 
            'message', 'Borsa fiyatı kabul edilebilir tavan fiyatı aştı',
            'current_price', v_unit_price,
            'max_acceptable_price', p_max_acceptable_price
        );
    END IF;

    -- 4. Check player balance if player exists in players table
    IF p_player_id IS NOT NULL AND p_player_id <> '' THEN
        SELECT money INTO v_player_money FROM public.players WHERE id = p_player_id FOR UPDATE;
        IF FOUND AND v_player_money < v_total_cost THEN
            RETURN jsonb_build_object(
                'success', false, 
                'error', 'INSUFFICIENT_FUNDS', 
                'message', 'Oyuncu bakiyesi yetersiz',
                'player_money', v_player_money,
                'total_cost', v_total_cost
            );
        END IF;

        IF FOUND THEN
            UPDATE public.players 
            SET money = money - v_total_cost,
                dollar_balance = dollar_balance - v_total_cost,
                daily_expense = daily_expense + v_total_cost,
                updated_at = NOW()
            WHERE id = p_player_id;
        END IF;
    END IF;

    -- 5. Calculate new stock and dynamic price following MacroEconomyEngine formula
    v_new_stock := GREATEST(v_stock - p_quantity, 0);
    v_safe_stock := v_new_stock::numeric;
    v_raw_base := v_base_price::numeric;

    IF v_safe_stock < v_s0 THEN
        v_stock_drop_ratio := (v_s0 - v_safe_stock) / v_s0;
        v_price_increase_factor := v_stock_drop_ratio * 4.0;
        v_computed_price := v_raw_base * (1.0 + v_price_increase_factor);
        v_computed_price := GREATEST(v_computed_price, v_raw_base + 1.0);
    ELSIF v_safe_stock > v_s0 THEN
        v_stock_drop_ratio := (v_safe_stock - v_s0) / v_s0;
        v_price_increase_factor := v_stock_drop_ratio * 2.0;
        v_computed_price := v_raw_base * GREATEST(0.10, 1.0 - v_price_increase_factor);
        v_computed_price := LEAST(v_computed_price, GREATEST(1.0, v_raw_base - 1.0));
    ELSE
        v_computed_price := v_raw_base;
    END IF;

    -- Crisis check (<= 999)
    IF v_new_stock <= 999 THEN
        v_computed_price := v_computed_price * 2.0;
    END IF;

    v_min_price := GREATEST(1, ROUND(v_raw_base * 0.10)::BIGINT);
    v_max_price := CASE WHEN v_new_stock <= 999 THEN ROUND(v_raw_base * 8.0)::BIGINT ELSE ROUND(v_raw_base * 4.0)::BIGINT END;
    v_new_price := LEAST(GREATEST(ROUND(v_computed_price)::BIGINT, v_min_price), v_max_price);

    -- 6. Update market_prices with new stock and new calculated price
    UPDATE public.market_prices
    SET stock = v_new_stock,
        borsa_stock = v_new_stock,
        current_price = v_new_price,
        updated_at = NOW()
    WHERE id = v_rec.id OR item_id = p_item_id;

    -- 7. Audit transaction
    INSERT INTO public.trade_transactions (
        transaction_type,
        buyer_id,
        item_id,
        quantity,
        unit_price,
        total_amount,
        city_id
    ) VALUES (
        'MARKET_BUY',
        p_player_id,
        p_item_id,
        p_quantity,
        v_unit_price,
        v_total_cost,
        'new_york'
    );

    RETURN jsonb_build_object(
        'success', true,
        'item_id', p_item_id,
        'quantity', p_quantity,
        'unit_price', v_unit_price,
        'total_cost', v_total_cost,
        'new_stock', v_new_stock,
        'new_price', v_new_price
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION execute_borsa_buy(TEXT, TEXT, INT, BIGINT) TO anon, authenticated, service_role;

