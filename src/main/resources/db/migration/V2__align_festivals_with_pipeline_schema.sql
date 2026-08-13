DO $$
BEGIN
    IF to_regclass('festivals') IS NULL THEN
        RETURN;
    END IF;

    ALTER TABLE festivals
        ADD COLUMN IF NOT EXISTS public_id UUID,
        ADD COLUMN IF NOT EXISTS series_id BIGINT,
        ADD COLUMN IF NOT EXISTS series_public_id UUID,
        ADD COLUMN IF NOT EXISTS festival_year INTEGER,
        ADD COLUMN IF NOT EXISTS detail_address VARCHAR(100),
        ADD COLUMN IF NOT EXISTS operation_start_time TIME,
        ADD COLUMN IF NOT EXISTS operation_end_time TIME,
        ADD COLUMN IF NOT EXISTS publication_status VARCHAR(30);

    UPDATE festivals
    SET public_id = MD5(
            festival_id::TEXT || ':' ||
            RANDOM()::TEXT || ':' ||
            CLOCK_TIMESTAMP()::TEXT
    )::UUID
    WHERE public_id IS NULL;

    UPDATE festivals
    SET festival_year = EXTRACT(YEAR FROM start_date)::INTEGER
    WHERE festival_year IS NULL
      AND start_date IS NOT NULL;

    UPDATE festivals
    SET publication_status = 'DRAFT'
    WHERE publication_status IS NULL;

    ALTER TABLE festivals
        ALTER COLUMN public_id SET NOT NULL,
        ALTER COLUMN public_id SET DEFAULT (
            MD5(RANDOM()::TEXT || CLOCK_TIMESTAMP()::TEXT)::UUID
        ),
        ALTER COLUMN publication_status SET DEFAULT 'DRAFT',
        ALTER COLUMN publication_status SET NOT NULL;

    CREATE UNIQUE INDEX IF NOT EXISTS uk_festivals_public_id
        ON festivals (public_id);

    CREATE UNIQUE INDEX IF NOT EXISTS uk_festivals_series_year
        ON festivals (series_id, festival_year)
        WHERE series_id IS NOT NULL
          AND festival_year IS NOT NULL;
END
$$;
