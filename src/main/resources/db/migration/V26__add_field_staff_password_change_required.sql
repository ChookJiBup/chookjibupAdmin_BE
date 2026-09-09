-- 스태프 본인 비밀번호 변경 API 도입에 따라, 관리자가 발급한 임시 비밀번호를
-- 그대로 쓰고 있는지 표시하는 컬럼을 추가한다.
-- 기존 계정은 모두 임시 비밀번호 상태이므로 true로 채운다.
DO $$
BEGIN
    IF to_regclass('field_staff_accounts') IS NULL THEN
        RAISE EXCEPTION
            'field_staff_accounts must exist before applying V26; provision the staff table first';
    END IF;

    ALTER TABLE field_staff_accounts
        ADD COLUMN IF NOT EXISTS password_change_required BOOLEAN;

    UPDATE field_staff_accounts
    SET password_change_required = TRUE
    WHERE password_change_required IS NULL;

    ALTER TABLE field_staff_accounts
        ALTER COLUMN password_change_required SET DEFAULT TRUE,
        ALTER COLUMN password_change_required SET NOT NULL;
END
$$;
