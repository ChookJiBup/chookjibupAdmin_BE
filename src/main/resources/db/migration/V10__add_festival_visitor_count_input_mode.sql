DO $$
BEGIN
    IF to_regclass('festivals') IS NULL THEN
        RETURN;
    END IF;

    ALTER TABLE festivals
        ADD COLUMN IF NOT EXISTS visitor_count_input_mode VARCHAR(20);

    UPDATE festivals
    SET visitor_count_input_mode = 'UNSET'
    WHERE visitor_count_input_mode IS NULL;

    IF to_regclass('festival_visitor_count') IS NOT NULL
       AND to_regclass('festival_visitor_total') IS NOT NULL THEN
        UPDATE festivals f
        SET visitor_count_input_mode = 'DAILY'
        WHERE f.visitor_count_input_mode = 'UNSET'
          AND EXISTS (
                SELECT 1
                FROM festival_visitor_count d
                WHERE d.festival_id = f.festival_id
          )
          AND NOT EXISTS (
                SELECT 1
                FROM festival_visitor_total t
                WHERE t.festival_id = f.festival_id
          );

        UPDATE festivals f
        SET visitor_count_input_mode = 'TOTAL'
        WHERE f.visitor_count_input_mode = 'UNSET'
          AND EXISTS (
                SELECT 1
                FROM festival_visitor_total t
                WHERE t.festival_id = f.festival_id
          )
          AND NOT EXISTS (
                SELECT 1
                FROM festival_visitor_count d
                WHERE d.festival_id = f.festival_id
          );

        UPDATE festivals f
        SET visitor_count_input_mode = 'DAILY'
        WHERE f.visitor_count_input_mode = 'UNSET'
          AND EXISTS (
                SELECT 1
                FROM festival_visitor_total t
                WHERE t.festival_id = f.festival_id
          )
          AND EXISTS (
                SELECT 1
                FROM festival_visitor_count d
                WHERE d.festival_id = f.festival_id
          )
          AND (
                SELECT COALESCE(SUM(d.visitor_count), 0)
                FROM festival_visitor_count d
                WHERE d.festival_id = f.festival_id
          ) = (
                SELECT t.total_visitor_count
                FROM festival_visitor_total t
                WHERE t.festival_id = f.festival_id
          );
    END IF;

    ALTER TABLE festivals
        ALTER COLUMN visitor_count_input_mode SET DEFAULT 'UNSET',
        ALTER COLUMN visitor_count_input_mode SET NOT NULL;
END
$$;
