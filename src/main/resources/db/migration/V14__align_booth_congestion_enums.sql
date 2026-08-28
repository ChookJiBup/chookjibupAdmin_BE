DO $$
BEGIN
    IF to_regclass('booth_congestion') IS NULL THEN
        RETURN;
    END IF;

    ALTER TABLE booth_congestion
        DROP CONSTRAINT IF EXISTS chk_booth_congestion_modifier;

    ALTER TABLE booth_congestion
        ALTER COLUMN modifier_type TYPE VARCHAR(20)
            USING UPPER(modifier_type::TEXT),
        ALTER COLUMN congestion_level TYPE VARCHAR(20)
            USING CASE LOWER(congestion_level::TEXT)
                WHEN 'comfortable' THEN 'LOW'
                WHEN 'normal' THEN 'MEDIUM'
                WHEN 'crowded' THEN 'HIGH'
                ELSE UPPER(congestion_level::TEXT)
            END;

    ALTER TABLE booth_congestion
        ADD CONSTRAINT chk_booth_congestion_modifier CHECK (
            (modifier_type = 'ADMIN'
                AND modifier_admin_id IS NOT NULL
                AND modifier_staff_id IS NULL)
            OR (modifier_type = 'STAFF'
                AND modifier_staff_id IS NOT NULL
                AND modifier_admin_id IS NULL)
        );

    ALTER TABLE booth_congestion
        ADD CONSTRAINT chk_booth_congestion_level CHECK (
            congestion_level IN ('LOW', 'MEDIUM', 'HIGH')
        );
END
$$;
