-- =============================================================================
-- MIGRATION: 20260926_add_quality_level.sql
-- Add quality_level (INTEGER 1-5) and calculated price columns to all product,
-- market, contract, transaction, and consortium tables.
-- =============================================================================

-- 1. Helper function for price multiplier
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

-- 2. Add quality_level to market_prices
ALTER TABLE IF EXISTS public.market_prices
    ADD COLUMN IF NOT EXISTS quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5);

-- 3. Add quality_level to global_market
ALTER TABLE IF EXISTS public.global_market
    ADD COLUMN IF NOT EXISTS quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    ADD COLUMN IF NOT EXISTS quality_tier TEXT NOT NULL DEFAULT 'Standart';

-- 4. Add quality_level to buy_orders
ALTER TABLE IF EXISTS public.buy_orders
    ADD COLUMN IF NOT EXISTS quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5),
    ADD COLUMN IF NOT EXISTS min_quality_level INTEGER NOT NULL DEFAULT 1 CHECK (min_quality_level BETWEEN 1 AND 5);

-- 5. Add quality_level to futures_contracts
ALTER TABLE IF EXISTS public.futures_contracts
    ADD COLUMN IF NOT EXISTS quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5);

-- 6. Add quality_level to trade_transactions
ALTER TABLE IF EXISTS public.trade_transactions
    ADD COLUMN IF NOT EXISTS quality_level INTEGER NOT NULL DEFAULT 1 CHECK (quality_level BETWEEN 1 AND 5);

-- 7. Add craftsmanship fields to global_guilds
ALTER TABLE IF EXISTS public.global_guilds
    ADD COLUMN IF NOT EXISTS slot_quality_levels JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS average_craftsmanship_score NUMERIC(4, 2) NOT NULL DEFAULT 1.0,
    ADD COLUMN IF NOT EXISTS master_craftsmanship_tier INTEGER NOT NULL DEFAULT 1 CHECK (master_craftsmanship_tier BETWEEN 1 AND 5),
    ADD COLUMN IF NOT EXISTS craftsmanship_multiplier NUMERIC(4, 2) NOT NULL DEFAULT 1.0;

-- 8. Add quality_level to museum_artifacts & museum_auctions
ALTER TABLE IF EXISTS public.museum_artifacts
    ADD COLUMN IF NOT EXISTS quality_level INTEGER NOT NULL DEFAULT 5 CHECK (quality_level BETWEEN 1 AND 5);

ALTER TABLE IF EXISTS public.museum_auctions
    ADD COLUMN IF NOT EXISTS quality_level INTEGER NOT NULL DEFAULT 5 CHECK (quality_level BETWEEN 1 AND 5);

-- 9. Update indexes
CREATE INDEX IF NOT EXISTS idx_market_prices_ql ON public.market_prices(item_id, quality_level);
CREATE INDEX IF NOT EXISTS idx_global_market_ql ON public.global_market(item_id, quality_level);
CREATE INDEX IF NOT EXISTS idx_buy_orders_ql ON public.buy_orders(item_id, quality_level);
CREATE INDEX IF NOT EXISTS idx_futures_ql ON public.futures_contracts(item_id, quality_level);
CREATE INDEX IF NOT EXISTS idx_trade_tx_ql ON public.trade_transactions(item_id, quality_level);
