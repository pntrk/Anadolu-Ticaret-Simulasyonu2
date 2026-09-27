-- =============================================================================
-- MIGRATION: 20260926_v1_26_add_quality_level_tables.sql
-- Sürüm 26 (v1.26): Add quality_level (INTEGER 1-5) to:
-- 1. products
-- 2. inventory
-- 3. consortium_production
-- 4. market_transactions
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. PRODUCTS TABLE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.products (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    base_price BIGINT NOT NULL DEFAULT 1000,
    category TEXT NOT NULL DEFAULT 'general',
    facility_id TEXT,
    tier INTEGER NOT NULL DEFAULT 1,
    quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    quality_tier TEXT NOT NULL DEFAULT 'star1',
    calculated_price BIGINT GENERATED ALWAYS AS (
        ROUND(base_price * CASE quality_level
            WHEN 1 THEN 1.00
            WHEN 2 THEN 1.35
            WHEN 3 THEN 1.90
            WHEN 4 THEN 2.80
            WHEN 5 THEN 4.50
            ELSE 1.00
        END)
    ) STORED,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Alter if existing
ALTER TABLE IF EXISTS public.products
    ADD COLUMN IF NOT EXISTS quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    ADD COLUMN IF NOT EXISTS quality_tier TEXT NOT NULL DEFAULT 'star1';

CREATE INDEX IF NOT EXISTS idx_products_quality_level ON public.products(id, quality_level);


-- -----------------------------------------------------------------------------
-- 2. INVENTORY TABLE (Player / Warehouse Stock)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.inventory (
    id TEXT PRIMARY KEY, -- 'player_id:item_id:quality_level' or 'item_id_quality'
    player_id TEXT NOT NULL DEFAULT 'local_player',
    item_id TEXT NOT NULL, -- e.g. 'steel' or 'wheat'
    unique_key TEXT NOT NULL, -- e.g. 'steel_star4'
    quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    quality_tier TEXT NOT NULL DEFAULT 'star1',
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    acquired_price BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Alter if existing
ALTER TABLE IF EXISTS public.inventory
    ADD COLUMN IF NOT EXISTS quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    ADD COLUMN IF NOT EXISTS quality_tier TEXT NOT NULL DEFAULT 'star1',
    ADD COLUMN IF NOT EXISTS unique_key TEXT;

CREATE INDEX IF NOT EXISTS idx_inventory_player_quality ON public.inventory(player_id, item_id, quality_level);


-- -----------------------------------------------------------------------------
-- 3. CONSORTIUM PRODUCTION TABLE (Serial Batch Production & Manufacturing Line)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.consortium_production (
    id TEXT PRIMARY KEY, -- Batch production UUID
    project_id TEXT NOT NULL, -- References global_guilds/mega_projects
    project_name TEXT NOT NULL DEFAULT '',
    target_product_id TEXT NOT NULL,
    target_product_name TEXT NOT NULL DEFAULT '',
    quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    target_quality_grade TEXT NOT NULL DEFAULT 'C' CHECK (target_quality_grade IN ('A', 'B', 'C', 'GRADE_A', 'GRADE_B', 'GRADE_C')),
    quality_tier TEXT NOT NULL DEFAULT 'GRADE_C',
    batch_index INTEGER NOT NULL DEFAULT 1,
    produced_quantity INTEGER NOT NULL DEFAULT 0 CHECK (produced_quantity >= 0),
    unit_batch_price BIGINT NOT NULL DEFAULT 0 CHECK (unit_batch_price >= 0),
    total_batch_value BIGINT GENERATED ALWAYS AS (produced_quantity * unit_batch_price) STORED,
    craftsmanship_multiplier NUMERIC(4, 2) NOT NULL DEFAULT 1.0,
    average_input_quality NUMERIC(4, 2) NOT NULL DEFAULT 1.0,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    started_at_ms BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
    completed_at_ms BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Alter if existing
ALTER TABLE IF EXISTS public.consortium_production
    ADD COLUMN IF NOT EXISTS quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    ADD COLUMN IF NOT EXISTS target_quality_grade TEXT NOT NULL DEFAULT 'C',
    ADD COLUMN IF NOT EXISTS quality_tier TEXT NOT NULL DEFAULT 'GRADE_C',
    ADD COLUMN IF NOT EXISTS average_input_quality NUMERIC(4, 2) NOT NULL DEFAULT 1.0;

CREATE INDEX IF NOT EXISTS idx_consortium_production_quality ON public.consortium_production(project_id, quality_level);


-- -----------------------------------------------------------------------------
-- 4. MARKET TRANSACTIONS TABLE (Trade / Sales / Order Execution Logs)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.market_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_type TEXT NOT NULL DEFAULT 'MARKET_TRADE', -- 'MARKET_BUY', 'MARKET_SELL', 'FUTURES_SETTLE', 'CONSORTIUM_SUPPLY'
    buyer_id TEXT,
    buyer_name TEXT,
    seller_id TEXT,
    seller_name TEXT,
    item_id TEXT NOT NULL,
    item_name TEXT DEFAULT '',
    quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    quality_tier TEXT NOT NULL DEFAULT 'star1',
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price BIGINT NOT NULL CHECK (unit_price >= 0),
    total_amount BIGINT NOT NULL CHECK (total_amount >= 0),
    city_id TEXT NOT NULL DEFAULT 'istanbul',
    synergy_bonus_percent INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'SETTLED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Alter if existing
ALTER TABLE IF EXISTS public.market_transactions
    ADD COLUMN IF NOT EXISTS quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    ADD COLUMN IF NOT EXISTS quality_tier TEXT NOT NULL DEFAULT 'star1',
    ADD COLUMN IF NOT EXISTS synergy_bonus_percent INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_market_transactions_quality ON public.market_transactions(item_id, quality_level);
CREATE INDEX IF NOT EXISTS idx_market_transactions_buyer ON public.market_transactions(buyer_id);
CREATE INDEX IF NOT EXISTS idx_market_transactions_seller ON public.market_transactions(seller_id);
CREATE INDEX IF NOT EXISTS idx_market_transactions_created ON public.market_transactions(created_at DESC);


-- -----------------------------------------------------------------------------
-- 5. ROW LEVEL SECURITY & POLICIES
-- -----------------------------------------------------------------------------
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.inventory ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.consortium_production ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.market_transactions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read products" ON public.products;
CREATE POLICY "Public read products" ON public.products FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public modify products" ON public.products;
CREATE POLICY "Public modify products" ON public.products FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read inventory" ON public.inventory;
CREATE POLICY "Public read inventory" ON public.inventory FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public modify inventory" ON public.inventory;
CREATE POLICY "Public modify inventory" ON public.inventory FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read consortium_production" ON public.consortium_production;
CREATE POLICY "Public read consortium_production" ON public.consortium_production FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public modify consortium_production" ON public.consortium_production;
CREATE POLICY "Public modify consortium_production" ON public.consortium_production FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read market_transactions" ON public.market_transactions;
CREATE POLICY "Public read market_transactions" ON public.market_transactions FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public modify market_transactions" ON public.market_transactions;
CREATE POLICY "Public modify market_transactions" ON public.market_transactions FOR ALL USING (true);
