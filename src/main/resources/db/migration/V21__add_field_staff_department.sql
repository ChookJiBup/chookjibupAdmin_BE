-- 현장 스태프 화면설계서(MGNT02)의 「근무구역」 입력값을 저장한다.
-- 기존 계정은 값이 없으므로 nullable로 추가한다.
DO $$
BEGIN
    IF to_regclass('field_staff_accounts') IS NULL THEN
        RAISE EXCEPTION
            'field_staff_accounts must exist before applying V21; provision the staff table first';
    END IF;

    ALTER TABLE field_staff_accounts
        ADD COLUMN IF NOT EXISTS department VARCHAR(100);
END
$$;
