DO $$
BEGIN
    IF to_regclass('field_staff_accounts') IS NULL THEN
        RAISE EXCEPTION
            'field_staff_accounts must exist before applying V14; provision the staff table first';
    END IF;

    -- 상태 변경 API가 사용하는 인증 버전 컬럼을 운영 DB에도 보장한다.
    ALTER TABLE field_staff_accounts
        ADD COLUMN IF NOT EXISTS auth_version BIGINT;

    UPDATE field_staff_accounts
    SET auth_version = 0
    WHERE auth_version IS NULL;

    ALTER TABLE field_staff_accounts
        ALTER COLUMN auth_version SET DEFAULT 0,
        ALTER COLUMN auth_version SET NOT NULL;

    -- JPA의 FieldStaffStatus(EnumType.STRING) 계약을 보장한다.
    ALTER TABLE field_staff_accounts
        ADD COLUMN IF NOT EXISTS status VARCHAR(50);

    UPDATE field_staff_accounts
    SET status = 'ACTIVE'
    WHERE status IS NULL;

    ALTER TABLE field_staff_accounts
        ALTER COLUMN status SET DEFAULT 'ACTIVE',
        ALTER COLUMN status SET NOT NULL;
END
$$;
