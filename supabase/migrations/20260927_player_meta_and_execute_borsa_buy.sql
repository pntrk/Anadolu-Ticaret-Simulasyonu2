-- =============================================================================
-- Migration: 20260927_player_meta_and_execute_borsa_buy.sql
-- Lightweight player metadata (player_meta) and atomic execute_borsa_buy RPC
-- =============================================================================

-- 1. Table: player_meta
CREATE TABLE IF NOT EXISTS public.player_meta (
    player_id TEXT PRIMARY KEY,
    name TEXT NOT NULL DEFAULT 'Tüccar',
    level INT NOT NULL DEFAULT 1,
    net_worth BIGINT NOT NULL DEFAULT 0,
    anti_cheat_hash TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Ensure stock & borsa_stock exist in market_prices
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

-- 2. Function: execute_borsa_buy with FOR UPDATE lock
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
