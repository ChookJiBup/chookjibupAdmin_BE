-- Retarget leftover pipeline FKs: admins(admin_id) -> admin_accounts(id)
DO $$
DECLARE
    fk RECORD;
    on_delete text;
BEGIN
    IF to_regclass('admin_accounts') IS NULL THEN
        RAISE EXCEPTION 'admin_accounts must exist before applying V18';
    END IF;

    FOR fk IN
        SELECT
            t.relname AS table_name,
            c.conname,
            (
                SELECT string_agg(quote_ident(a.attname), ', ' ORDER BY x.ordinality)
                FROM unnest(c.conkey) WITH ORDINALITY AS x(attnum, ordinality)
                JOIN pg_attribute a
                  ON a.attrelid = c.conrelid
                 AND a.attnum = x.attnum
            ) AS col_list,
            CASE c.confdeltype
                WHEN 'c' THEN 'CASCADE'
                WHEN 'n' THEN 'SET NULL'
                WHEN 'd' THEN 'SET DEFAULT'
                WHEN 'r' THEN 'RESTRICT'
                ELSE 'NO ACTION'
            END AS on_delete
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        JOIN pg_class ft ON ft.oid = c.confrelid
        WHERE c.contype = 'f'
          AND n.nspname = current_schema()
          AND ft.relname = 'admins'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I DROP CONSTRAINT %I',
            fk.table_name,
            fk.conname
        );

        BEGIN
            EXECUTE format(
                'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%s) REFERENCES admin_accounts (id) ON DELETE %s',
                fk.table_name,
                fk.conname,
                fk.col_list,
                fk.on_delete
            );
        EXCEPTION
            WHEN others THEN
                -- 기존 orphan 행이 있으면 NOT VALID로 붙이고 시드/운영 정리 후 검증한다.
                EXECUTE format(
                    'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%s) REFERENCES admin_accounts (id) ON DELETE %s NOT VALID',
                    fk.table_name,
                    fk.conname,
                    fk.col_list,
                    fk.on_delete
                );
        END;
    END LOOP;
END
$$;
