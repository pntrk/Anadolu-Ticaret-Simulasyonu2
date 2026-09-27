-- =============================================================================
-- MIGRATION: 20260926_add_consortium_quality_grade.sql
-- Consortium Target Quality Grade (A, B, C) and Input Acceptance Rule:
-- A Grade -> Only 4★ (Superior) and 5★ (Flawless) inputs allowed [4, 5]
-- B Grade -> Only 2★ (Select), 3★ (Masterwork), and 4★ (Superior) inputs allowed [2, 3, 4]
-- C Grade -> Only 1★ (Standard) and 2★ (Select) inputs allowed [1, 2]
-- =============================================================================

-- 1. Helper function for Consortium Input Acceptance Check
CREATE OR REPLACE FUNCTION is_consortium_input_quality_allowed(
    p_target_grade TEXT,
    p_input_quality_level INTEGER
) RETURNS BOOLEAN AS $$
BEGIN
    IF p_target_grade IN ('A', 'GRADE_A') THEN
        RETURN p_input_quality_level IN (4, 5);
    ELSIF p_target_grade IN ('B', 'GRADE_B') THEN
        RETURN p_input_quality_level IN (2, 3, 4);
    ELSE
        RETURN p_input_quality_level IN (1, 2);
    END IF;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- 2. Add target_quality_grade & quality_tier to global_guilds
ALTER TABLE IF EXISTS public.global_guilds
    ADD COLUMN IF NOT EXISTS target_quality_grade TEXT NOT NULL DEFAULT 'C' CHECK (target_quality_grade IN ('A', 'B', 'C', 'GRADE_A', 'GRADE_B', 'GRADE_C')),
    ADD COLUMN IF NOT EXISTS quality_tier TEXT NOT NULL DEFAULT 'GRADE_C',
    ADD COLUMN IF NOT EXISTS allowed_input_qualities TEXT NOT NULL DEFAULT '[1,2]';

-- 3. Add constraint trigger or check on consortium supply transactions
CREATE OR REPLACE FUNCTION check_consortium_supply_quality()
RETURNS TRIGGER AS $$
DECLARE
    v_guild_grade TEXT;
BEGIN
    IF NEW.transaction_type = 'CONSORTIUM_SUPPLY' AND NEW.seller_id IS NOT NULL THEN
        SELECT target_quality_grade INTO v_guild_grade FROM public.global_guilds WHERE id = NEW.buyer_id;
        IF v_guild_grade IS NOT NULL AND NOT is_consortium_input_quality_allowed(v_guild_grade, NEW.quality_level) THEN
            RAISE EXCEPTION 'Girdi kalitesi (% yıldız) bu konsorsiyumun % kalite standardına uymuyor!', NEW.quality_level, v_guild_grade;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_check_consortium_supply_quality ON public.trade_transactions;
CREATE TRIGGER trg_check_consortium_supply_quality
BEFORE INSERT ON public.trade_transactions
FOR EACH ROW EXECUTE FUNCTION check_consortium_supply_quality();
