DO $$
BEGIN
    IF to_regclass('festivals') IS NULL THEN
        RETURN;
    END IF;

    ALTER TABLE festivals
        ADD COLUMN IF NOT EXISTS festival_year INTEGER;

    UPDATE festivals
    SET festival_year = EXTRACT(YEAR FROM start_date)::INTEGER
    WHERE festival_year IS NULL
      AND start_date IS NOT NULL;

END
$$;
