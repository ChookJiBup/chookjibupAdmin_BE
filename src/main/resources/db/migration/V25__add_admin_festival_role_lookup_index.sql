-- 제2관리자 후보 조회는 admin_accounts LEFT JOIN admin_festival_roles(festival_id = ?)로
-- 이미 배정된 계정을 걸러낸다. 기존 인덱스는 (admin_account_id, festival_id) 유니크뿐이라
-- festival_id만으로 좁힐 수 없어 축제별 역할 조회가 전체 스캔으로 떨어진다.
CREATE INDEX IF NOT EXISTS idx_admin_festival_roles_festival_account
    ON admin_festival_roles (festival_id, admin_account_id);

-- 제2관리자 목록 조회(festival_id + invited_by_admin_id + role) 전용 인덱스.
CREATE INDEX IF NOT EXISTS idx_admin_festival_roles_festival_inviter
    ON admin_festival_roles (festival_id, invited_by_admin_id);
