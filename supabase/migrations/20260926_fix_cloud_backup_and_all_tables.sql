-- =============================================================================
-- MIGRATION: 20260926_fix_cloud_backup_and_all_tables.sql
-- Complete Fix for Cloud Backup, Player Save Schemas, Market Prices, and Missing Tables
-- =============================================================================

-- 1. PLAYERS TABLE: Ensure all columns exist idempotently
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

-- 2. MARKET_PRICES TABLE: Ensure all columns exist idempotently
ALTER TABLE IF EXISTS public.market_prices
    ADD COLUMN IF NOT EXISTS total_volume BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS origin_country TEXT DEFAULT 'Türkiye',
    ADD COLUMN IF NOT EXISTS borsa_stock BIGINT NOT NULL DEFAULT 1000,
    ADD COLUMN IF NOT EXISTS stock BIGINT NOT NULL DEFAULT 1000,
    ADD COLUMN IF NOT EXISTS is_usd BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. RLS POLICIES FOR PLAYERS (Full Access for Cloud Backup)
ALTER TABLE public.players ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read players" ON public.players;
CREATE POLICY "Public read players" ON public.players FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public update players" ON public.players;
CREATE POLICY "Public update players" ON public.players FOR ALL USING (true) WITH CHECK (true);
