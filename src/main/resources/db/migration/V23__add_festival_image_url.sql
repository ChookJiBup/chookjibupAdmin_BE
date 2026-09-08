-- 사용자 목록/상세/찜 화면에서 함께 사용하는 공개 포스터 주소.
-- 사용자 백엔드의 image_url 엔티티 매핑 배포 전에 적용한다.
ALTER TABLE festivals ADD COLUMN IF NOT EXISTS image_url TEXT;
