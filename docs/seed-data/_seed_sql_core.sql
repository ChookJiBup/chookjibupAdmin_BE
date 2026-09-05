-- 생성 소스 파일: 직접 실행하지 말고 _generate_seed_sql.ps1로 통합 파일을 만든다.
-- __BCRYPT__ / __SHA256__ placeholder가 남은 상태로 실행하면 로그인·파일 검증이 실패한다.

-- ========== mapping & cleanup ==========
DROP TABLE IF EXISTS pg_temp.seed_festival_map;
DROP TABLE IF EXISTS pg_temp.seed_admin_ids;
DROP TABLE IF EXISTS pg_temp.seed_staff_ids;
-- progress_status / is_active / lat·lng 는 Admin RDS festivals에 없을 수 있다.
-- 진행 상태는 start_date·end_date로 계산하고, 좌표는 시드 기본값을 쓴다.
CREATE TEMP TABLE seed_festival_map AS
WITH ranked AS (
    SELECT
        festival_id,
        public_id,
        festival_name,
        CASE
            WHEN start_date IS NULL OR end_date IS NULL THEN NULL
            WHEN CURRENT_DATE < start_date THEN 'upcoming'
            WHEN CURRENT_DATE > end_date THEN 'completed'
            ELSE 'ongoing'
        END AS progress_status,
        start_date,
        end_date,
        37.5665000::numeric AS base_lat,
        126.9780000::numeric AS base_lng,
        road_address
    FROM festivals
    WHERE start_date IS NOT NULL
      AND end_date IS NOT NULL
)
SELECT
    ROW_NUMBER() OVER (
        ORDER BY
            CASE progress_status WHEN 'ongoing' THEN 0 WHEN 'upcoming' THEN 1 ELSE 2 END,
            start_date NULLS LAST,
            festival_id
    ) AS seed_idx,
    ROW_NUMBER() OVER (
        PARTITION BY progress_status
        ORDER BY start_date NULLS LAST, festival_id
    ) AS status_rank,
    festival_id,
    public_id,
    festival_name,
    progress_status,
    start_date,
    end_date,
    base_lat,
    base_lng,
    road_address
FROM ranked
WHERE progress_status IN ('ongoing', 'upcoming', 'completed')
LIMIT 10;

DO $$
BEGIN
    IF (SELECT COUNT(*) FROM seed_festival_map) < 10 THEN
        RAISE EXCEPTION 'seed_festival_map has % rows; need 10', (SELECT COUNT(*) FROM seed_festival_map);
    END IF;
END $$;

-- 시드 namespace 계정만 정리한다. 운영 계정 전체를 도메인 패턴으로 삭제하지 않는다.
CREATE TEMP TABLE seed_admin_ids ON COMMIT DROP AS
SELECT id
FROM admin_accounts
WHERE email LIKE 'owner.%@seed.mapo.go.kr'
   OR email LIKE 'op.%@seed.mapo.go.kr'
   OR email LIKE 'contractor.%@seed-event.co.kr'
   OR email LIKE 'admin%@seed.mapo.go.kr'
   OR email LIKE 'admin%@seed-event.co.kr';

CREATE TEMP TABLE seed_staff_ids ON COMMIT DROP AS
SELECT id
FROM field_staff_accounts
WHERE login_id LIKE 'staff-__-__';

-- scoped cleanup (re-run safe for the fixed seed namespace)
DELETE FROM booth_congestion bc
USING booth_info bi, seed_festival_map sf
WHERE bc.booth_id = bi.booth_id AND bi.festival_id = sf.festival_id;

DELETE FROM booth_queue bq
USING booth_info bi, seed_festival_map sf
WHERE bq.booth_id = bi.booth_id AND bi.festival_id = sf.festival_id;

DELETE FROM booth_info bi
USING seed_festival_map sf
WHERE bi.festival_id = sf.festival_id;

DELETE FROM roadmap_node rn
USING festival_roadmap fr, seed_festival_map sf
WHERE rn.roadmap_id = fr.id AND fr.festival_id = sf.festival_id;

DELETE FROM festival_roadmap fr
USING seed_festival_map sf
WHERE fr.festival_id = sf.festival_id;

DELETE FROM festival_maps fm
USING seed_festival_map sf
WHERE fm.festival_id = sf.festival_id;

DELETE FROM festival_locations fl
USING seed_festival_map sf
WHERE fl.festival_id = sf.festival_id;

DELETE FROM festival_visitor_count fvc
USING seed_festival_map sf
WHERE fvc.festival_id = sf.festival_id;

DELETE FROM festival_visitor_total fvt
USING seed_festival_map sf
WHERE fvt.festival_id = sf.festival_id;

DELETE FROM field_staff_accounts fsa
USING seed_festival_map sf
WHERE fsa.festival_id = sf.festival_id;

-- 매핑 축제가 바뀌어도 이전 실행의 시드 관계가 계정을 붙잡지 않도록 정리한다.
DELETE FROM booth_congestion bc
USING seed_staff_ids ss
WHERE bc.modifier_staff_id = ss.id;

DELETE FROM booth_queue bq
USING seed_staff_ids ss
WHERE bq.modifier_staff_id = ss.id;

DELETE FROM field_staff_accounts fsa
USING seed_staff_ids ss
WHERE fsa.id = ss.id;

DELETE FROM admin_festival_roles afr
USING seed_admin_ids sa
WHERE afr.admin_account_id = sa.id
   OR afr.invited_by_admin_id = sa.id;

DELETE FROM admin_accounts aa
USING seed_admin_ids sa
WHERE aa.id = sa.id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM admin_accounts WHERE id BETWEEN 1 AND 48) THEN
        RAISE EXCEPTION 'Reserved seed admin ids 1..48 are already used by non-seed accounts.';
    END IF;
END $$;

-- visitor input mode on pipeline festivals
UPDATE festivals f
SET visitor_count_input_mode = CASE
    WHEN m.progress_status = 'ongoing' THEN 'DAILY'
    WHEN m.progress_status = 'upcoming' THEN 'UNSET'
    WHEN m.progress_status = 'completed' AND m.status_rank <= 2 THEN 'DAILY'
    ELSE 'TOTAL'
END
FROM seed_festival_map m
WHERE f.festival_id = m.festival_id;

-- ========== admin_accounts (30 bulk + 18 fixture = 48) ==========
INSERT INTO admin_accounts (
    id, public_id, account_kind, email, name, organization, job_rank,
    password_hash, auth_version, status, created_at, updated_at
)
SELECT
    g.id,
    ('a0000000-0000-4000-8000-' || lpad(g.id::text, 12, '0'))::uuid,
    CASE WHEN g.id BETWEEN 21 AND 30 THEN 'CONTRACTOR' ELSE 'GOVERNMENT' END,
    CASE
        WHEN g.id BETWEEN 1 AND 10 THEN 'owner.' || lpad(g.id::text, 2, '0') || '@seed.mapo.go.kr'
        WHEN g.id BETWEEN 11 AND 20 THEN 'op.' || lpad((g.id - 10)::text, 2, '0') || '@seed.mapo.go.kr'
        ELSE 'contractor.' || lpad((g.id - 20)::text, 2, '0') || '@seed-event.co.kr'
    END,
    CASE
        WHEN g.id BETWEEN 1 AND 10 THEN (ARRAY['김','이','박','최','정','강','조','윤','장','임'])[((g.id - 1) % 10) + 1] || '총괄' || lpad(g.id::text, 2, '0')
        WHEN g.id BETWEEN 11 AND 20 THEN (ARRAY['김','이','박','최','정','강','조','윤','장','임'])[((g.id - 11) % 10) + 1] || '운영' || lpad((g.id - 10)::text, 2, '0')
        ELSE '외부운영' || lpad((g.id - 20)::text, 2, '0')
    END,
    CASE
        WHEN g.id BETWEEN 1 AND 10 THEN (ARRAY['관광정책과','문화관광과','축제추진단','도시디자인과','안전총괄과'])[((g.id - 1) % 5) + 1]
        WHEN g.id BETWEEN 11 AND 20 THEN (ARRAY['관광정책과','도시디자인과','안전총괄과'])[((g.id - 11) % 3) + 1]
        ELSE '이벤트업체' || lpad((g.id - 20)::text, 2, '0')
    END,
    CASE
        WHEN g.id BETWEEN 21 AND 30 THEN NULL
        WHEN g.id % 7 = 0 THEN '단장'
        ELSE (ARRAY['과장','주무관','대리','사무관'])[((g.id - 1) % 4) + 1]
    END,
    '__BCRYPT__', 0, 'ACTIVE', now(), now()
FROM generate_series(1, 30) AS g(id);

INSERT INTO admin_accounts (
    id, public_id, account_kind, email, name, organization, job_rank,
    password_hash, auth_version, status, created_at, updated_at
)
VALUES
    (31, 'b0000031-0000-4000-8000-000000000031', 'GOVERNMENT', 'admin01@seed.mapo.go.kr', '연결테스트01', '관광정책과', '과장', '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (32, 'b0000032-0000-4000-8000-000000000032', 'GOVERNMENT', 'admin02@seed.mapo.go.kr', '연결테스트02', '관광정책과', '주무관', '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (33, 'b0000033-0000-4000-8000-000000000033', 'CONTRACTOR', 'admin03@seed-event.co.kr', '연결테스트03', '이벤트업체A', NULL, '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (34, 'b0000034-0000-4000-8000-000000000034', 'GOVERNMENT', 'admin11@seed.mapo.go.kr', '미연결11', '문화관광과', '과장', '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (35, 'b0000035-0000-4000-8000-000000000035', 'GOVERNMENT', 'admin12@seed.mapo.go.kr', '미연결12', '문화관광과', '주무관', '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (36, 'b0000036-0000-4000-8000-000000000036', 'CONTRACTOR', 'admin13@seed-event.co.kr', '미연결13', '이벤트업체B', NULL, '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (37, 'b0000037-0000-4000-8000-000000000037', 'GOVERNMENT', 'admin21@seed.mapo.go.kr', '세트C01', '축제추진단', '과장', '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (38, 'b0000038-0000-4000-8000-000000000038', 'GOVERNMENT', 'admin22@seed.mapo.go.kr', '세트C02', '축제추진단', '주무관', '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (39, 'b0000039-0000-4000-8000-000000000039', 'CONTRACTOR', 'admin23@seed-event.co.kr', '세트C03', '이벤트업체C', NULL, '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (40, 'b0000040-0000-4000-8000-000000000040', 'GOVERNMENT', 'admin31@seed.mapo.go.kr', '세트D01', '도시디자인과', '과장', '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (41, 'b0000041-0000-4000-8000-000000000041', 'GOVERNMENT', 'admin32@seed.mapo.go.kr', '세트D02', '도시디자인과', '대리', '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (42, 'b0000042-0000-4000-8000-000000000042', 'CONTRACTOR', 'admin33@seed-event.co.kr', '세트D03', '이벤트업체D', NULL, '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (43, 'b0000043-0000-4000-8000-000000000043', 'GOVERNMENT', 'admin41@seed.mapo.go.kr', '세트E01', '안전총괄과', '과장', '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (44, 'b0000044-0000-4000-8000-000000000044', 'GOVERNMENT', 'admin42@seed.mapo.go.kr', '세트E02', '안전총괄과', '주무관', '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (45, 'b0000045-0000-4000-8000-000000000045', 'CONTRACTOR', 'admin43@seed-event.co.kr', '세트E03', '이벤트업체E', NULL, '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (46, 'b0000046-0000-4000-8000-000000000046', 'GOVERNMENT', 'admin51@seed.mapo.go.kr', '세트F01', '관광정책과', '과장', '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (47, 'b0000047-0000-4000-8000-000000000047', 'GOVERNMENT', 'admin52@seed.mapo.go.kr', '세트F02', '관광정책과', '대리', '__BCRYPT__', 0, 'ACTIVE', now(), now()),
    (48, 'b0000048-0000-4000-8000-000000000048', 'CONTRACTOR', 'admin53@seed-event.co.kr', '세트F03', '이벤트업체F', NULL, '__BCRYPT__', 0, 'ACTIVE', now(), now());

-- ========== admin_festival_roles (30 base + 10 A-cross = 40) ==========
INSERT INTO admin_festival_roles (
    public_id, admin_account_id, festival_id, role, invited_by_admin_id, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    CASE m.seed_idx
        WHEN 1 THEN 31 WHEN 7 THEN 37 WHEN 8 THEN 40 WHEN 9 THEN 43 WHEN 10 THEN 46
        ELSE m.seed_idx
    END,
    m.festival_id,
    'FESTIVAL_OWNER',
    NULL,
    now(), now()
FROM seed_festival_map m;

INSERT INTO admin_festival_roles (public_id, admin_account_id, festival_id, role, invited_by_admin_id, created_at, updated_at)
SELECT gen_random_uuid(), sub.admin_id, m.festival_id, 'SUB_ADMIN', sub.owner_id, now(), now()
FROM seed_festival_map m
CROSS JOIN LATERAL (
    VALUES
        (CASE m.seed_idx WHEN 1 THEN 32 WHEN 7 THEN 38 WHEN 8 THEN 41 WHEN 9 THEN 44 WHEN 10 THEN 47 ELSE 10 + m.seed_idx END),
         CASE m.seed_idx WHEN 1 THEN 31 WHEN 7 THEN 37 WHEN 8 THEN 40 WHEN 9 THEN 43 WHEN 10 THEN 46 ELSE m.seed_idx END),
        (CASE m.seed_idx WHEN 1 THEN 33 WHEN 7 THEN 39 WHEN 8 THEN 42 WHEN 9 THEN 45 WHEN 10 THEN 48 ELSE 20 + m.seed_idx END),
         CASE m.seed_idx WHEN 1 THEN 31 WHEN 7 THEN 37 WHEN 8 THEN 40 WHEN 9 THEN 43 WHEN 10 THEN 46 ELSE m.seed_idx END)
) AS sub(admin_id, owner_id);

-- A-set cross SUB_ADMIN (F2~F6)
INSERT INTO admin_festival_roles (public_id, admin_account_id, festival_id, role, invited_by_admin_id, created_at, updated_at)
SELECT gen_random_uuid(), x.admin_id, m.festival_id, 'SUB_ADMIN', m.seed_idx, now(), now()
FROM seed_festival_map m
JOIN LATERAL (
    SELECT unnest(CASE m.seed_idx
        WHEN 2 THEN ARRAY[31, 32]
        WHEN 3 THEN ARRAY[32, 33]
        WHEN 4 THEN ARRAY[31, 33]
        WHEN 5 THEN ARRAY[31, 32]
        WHEN 6 THEN ARRAY[32, 33]
        ELSE ARRAY[]::bigint[]
    END) AS admin_id
) x ON true
WHERE m.seed_idx BETWEEN 2 AND 6;

-- ========== field_staff_accounts (20) ==========
INSERT INTO field_staff_accounts (
    public_id, festival_id, login_id, name, phone_number, password_hash,
    auth_version, valid_from, valid_until, status, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    m.festival_id,
    'staff-' || lpad(m.seed_idx::text, 2, '0') || '-' || lpad(s.seq::text, 2, '0'),
    '스태프' || lpad(m.seed_idx::text, 2, '0') || '-' || lpad(s.seq::text, 2, '0'),
    '010-40' || lpad((g.staff_id)::text, 3, '0') || '-' || lpad(g.staff_id::text, 4, '0'),
    '__BCRYPT__', 0,
    (COALESCE(m.start_date, CURRENT_DATE) - interval '1 day')::timestamp,
    (COALESCE(m.end_date, CURRENT_DATE) + interval '1 day')::timestamp + time '23:59:59',
    CASE
        WHEN g.staff_id % 11 = 0 THEN 'DELETED'
        WHEN g.staff_id % 7 = 0 THEN 'INACTIVE'
        ELSE 'ACTIVE'
    END,
    now(), now()
FROM generate_series(1, 20) AS g(staff_id)
JOIN seed_festival_map m ON m.seed_idx = ((g.staff_id - 1) / 2) + 1
CROSS JOIN LATERAL (VALUES (1), (2)) AS s(seq)
WHERE ((g.staff_id - 1) % 2) + 1 = s.seq;

-- ========== festival_locations (25) ==========
INSERT INTO festival_locations (
    public_id, festival_id, location_type, location_name, road_address, jibun_address,
    detail_address, postal_code, latitude, longitude, boundary_geometry,
    source_type, is_primary, sort_order, created_by_admin_id, last_modified_by_admin_id,
    created_at, updated_at
)
SELECT
    gen_random_uuid(),
    f.festival_id,
    CASE WHEN f.n = 1 THEN 'MAIN_VENUE' ELSE (ARRAY['SUB_VENUE','STAGE_AREA','EXPERIENCE_AREA','PARKING','ENTRANCE'])[((f.n - 2) % 5) + 1] END,
    f.festival_name || CASE WHEN f.n = 1 THEN ' 주행사장' ELSE ' 부행사장-' || f.n END,
    COALESCE(f.road_address, f.festival_name || ' 도로명주소'),
    NULL,
    CASE WHEN f.n % 3 = 0 THEN NULL ELSE '상세 ' || f.n END,
    CASE WHEN f.n % 3 = 0 THEN NULL ELSE lpad((03900 + f.seed_idx * 10 + f.n)::text, 5, '0') END,
    f.base_lat + (f.seed_idx * 0.001) + (f.n * 0.0001),
    f.base_lng + (f.seed_idx * 0.001) + (f.n * 0.0001),
    CASE WHEN f.n % 5 = 0 THEN '{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,1],[0,0]]]}'::jsonb ELSE NULL END,
    CASE WHEN f.n = 1 THEN 'API' ELSE 'MANUAL' END,
    f.n = 1,
    f.n - 1,
    CASE WHEN f.n = 1 THEN NULL ELSE COALESCE(
        CASE f.seed_idx WHEN 1 THEN 31 WHEN 7 THEN 37 WHEN 8 THEN 40 WHEN 9 THEN 43 WHEN 10 THEN 46 ELSE f.seed_idx END, 1)
    END,
    CASE WHEN f.n = 1 THEN NULL ELSE COALESCE(
        CASE f.seed_idx WHEN 1 THEN 31 WHEN 7 THEN 37 WHEN 8 THEN 40 WHEN 9 THEN 43 WHEN 10 THEN 46 ELSE f.seed_idx END, 1)
    END,
    now(), now()
FROM (
    SELECT m.*, gs.n
    FROM seed_festival_map m
    JOIN LATERAL generate_series(1, CASE
        WHEN m.seed_idx <= 3 THEN 1
        WHEN m.seed_idx <= 6 THEN 2
        WHEN m.seed_idx <= 8 THEN 3
        ELSE 5
    END) gs(n) ON true
) f;

-- ========== festival_maps (13) ==========
INSERT INTO festival_maps (
    public_id, festival_id, location_id, map_name, original_file_name,
    source_image_key, display_image_key, analysis_image_key,
    source_content_type, display_content_type, analysis_content_type,
    source_file_size, display_file_size, analysis_file_size,
    image_width, image_height, analysis_image_width, analysis_image_height,
    source_checksum_sha256, display_checksum_sha256, analysis_checksum_sha256,
    storage_status, map_kind, is_current, created_by_admin_id, version,
    created_at, updated_at
)
SELECT
    gen_random_uuid(),
    m.festival_id,
    (SELECT fl.location_id FROM festival_locations fl WHERE fl.festival_id = m.festival_id AND fl.is_primary = true LIMIT 1),
    m.festival_name || ' 지도',
    'seed-map-' || m.seed_idx || '.png',
    'seed/maps/' || m.festival_id || '/original.png',
    'seed/maps/' || m.festival_id || '/display.png',
    'seed/maps/' || m.festival_id || '/analysis.png',
    'image/png', 'image/png', 'image/png',
    102400, 102400, 51200,
    1920, 1080, 960, 540,
    '__SHA256__', '__SHA256__', '__SHA256__',
    'UPLOADED',
    CASE WHEN m.seed_idx IN (1, 2) THEN 'COORDINATE' ELSE 'IMAGE' END,
    true,
    COALESCE(CASE m.seed_idx WHEN 1 THEN 31 WHEN 7 THEN 37 WHEN 8 THEN 40 WHEN 9 THEN 43 WHEN 10 THEN 46 ELSE m.seed_idx END, 1),
    0,
    now(), now()
FROM seed_festival_map m;

INSERT INTO festival_maps (
    public_id, festival_id, location_id, map_name, original_file_name,
    source_image_key, display_image_key, analysis_image_key,
    source_content_type, display_content_type, analysis_content_type,
    source_file_size, display_file_size, analysis_file_size,
    image_width, image_height, analysis_image_width, analysis_image_height,
    source_checksum_sha256, display_checksum_sha256, analysis_checksum_sha256,
    storage_status, map_kind, is_current, created_by_admin_id, replaces_map_id,
    replaced_at, version, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    m.festival_id,
    (SELECT fl.location_id FROM festival_locations fl WHERE fl.festival_id = m.festival_id AND fl.is_primary = true LIMIT 1),
    m.festival_name || ' 이전지도',
    'seed-map-old-' || m.seed_idx || '.png',
    'seed/maps/' || m.festival_id || '/old/original.png',
    'seed/maps/' || m.festival_id || '/old/display.png',
    'seed/maps/' || m.festival_id || '/old/analysis.png',
    'image/png', 'image/png', 'image/png',
    102400, 102400, 51200,
    1920, 1080, 960, 540,
    '__SHA256__', '__SHA256__', '__SHA256__',
    'REPLACED', 'IMAGE', false,
    COALESCE(CASE m.seed_idx WHEN 8 THEN 40 WHEN 9 THEN 43 WHEN 10 THEN 46 ELSE m.seed_idx END, 1),
    NULL,
    now(), 0, now(), now()
FROM seed_festival_map m
JOIN festival_maps cur ON cur.festival_id = m.festival_id AND cur.is_current = true
WHERE m.seed_idx IN (8, 9, 10);

-- 도메인 규칙과 동일하게 새(current) 지도가 이전(REPLACED) 지도를 가리킨다.
UPDATE festival_maps current_map
SET replaces_map_id = old_map.id
FROM seed_festival_map m
JOIN festival_maps old_map
  ON old_map.festival_id = m.festival_id
 AND old_map.is_current = false
 AND old_map.source_image_key = 'seed/maps/' || m.festival_id || '/old/original.png'
WHERE current_map.festival_id = m.festival_id
  AND current_map.is_current = true
  AND m.seed_idx IN (8, 9, 10);

-- ========== festival_roadmap (10) ==========
INSERT INTO festival_roadmap (
    public_id, festival_id, current_map_id, status, edit_revision, published_version,
    zones, created_by_admin_id, version, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    m.festival_id,
    fm.id,
    (ARRAY['ANALYZING','REVIEW_REQUIRED','REVIEW_REQUIRED','EDITING','EDITING','EDITING','EDITING','EDITING','PUBLISHED','PUBLISHED'])[m.seed_idx],
    (ARRAY[0,2,1,3,5,2,4,1,3,5])[m.seed_idx],
    (ARRAY[0,1,0,1,3,1,2,1,2,3])[m.seed_idx],
    jsonb_build_array(
        jsonb_build_object('zoneId', 'zone-main', 'name', '메인', 'color', '#FF6B6B'),
        jsonb_build_object('zoneId', 'zone-food', 'name', '푸드', 'color', '#4ECDC4'),
        jsonb_build_object('zoneId', 'zone-safe', 'name', '안전', 'color', '#FFE66D')
    ),
    COALESCE(CASE m.seed_idx WHEN 1 THEN 31 WHEN 7 THEN 37 WHEN 8 THEN 40 WHEN 9 THEN 43 WHEN 10 THEN 46 ELSE m.seed_idx END, 1),
    0, now(), now()
FROM seed_festival_map m
JOIN festival_maps fm ON fm.festival_id = m.festival_id AND fm.is_current = true;

-- ========== roadmap_node (150) + booth_info (80) ==========
INSERT INTO roadmap_node (
    public_id, roadmap_id, map_id, node_type, node_name, geometry_type, geometry_data,
    geometry_schema_version, confidence, recognized_text, source, review_status,
    sort_order, created_by_admin_id, last_modified_by_admin_id, version, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    fr.id,
    fr.current_map_id,
    CASE WHEN n.node_no <= 8 THEN 'BOOTH' ELSE (ARRAY['STAGE','RESTROOM','PATH','ENTRANCE','INFORMATION','PARKING','OTHER'])[n.node_no - 8] END,
    CASE WHEN n.node_no <= 8 THEN '부스-' || m.seed_idx || '-' || n.node_no ELSE '시설-' || m.seed_idx || '-' || n.node_no END,
    CASE
        WHEN fm.map_kind = 'COORDINATE' THEN 'POINT'
        WHEN n.node_no <= 8 THEN 'RECTANGLE'
        WHEN n.node_no IN (9, 12) THEN 'POINT'
        WHEN n.node_no = 11 THEN 'POLYLINE'
        ELSE 'POLYGON'
    END,
    CASE
        WHEN fm.map_kind = 'COORDINATE' THEN jsonb_build_object(
            'lat', m.base_lat + n.node_no * 0.00005,
            'lng', m.base_lng + n.node_no * 0.00005
        )
        WHEN n.node_no <= 8 THEN jsonb_build_object(
            'x', pos.x, 'y', pos.y, 'width', 0.12, 'height', 0.12, 'rotation', 0
        )
        WHEN n.node_no IN (9, 12) THEN jsonb_build_object('x', pos.x, 'y', pos.y)
        WHEN n.node_no = 11 THEN jsonb_build_object(
            'points', jsonb_build_array(
                jsonb_build_object('x', pos.x, 'y', pos.y),
                jsonb_build_object('x', pos.x + 0.08, 'y', pos.y + 0.03)
            )
        )
        ELSE jsonb_build_object(
            'points', jsonb_build_array(
                jsonb_build_object('x', pos.x, 'y', pos.y),
                jsonb_build_object('x', pos.x + 0.08, 'y', pos.y),
                jsonb_build_object('x', pos.x + 0.04, 'y', pos.y + 0.08)
            )
        )
    END,
    CASE WHEN fm.map_kind = 'COORDINATE' THEN '2.0' ELSE '1.0' END,
    CASE WHEN n.node_no > 8 AND n.node_no % 4 = 0 THEN 0.61 ELSE NULL END,
    CASE WHEN n.node_no > 8 AND n.node_no % 4 = 0 THEN 'AI인식텍스트' ELSE NULL END,
    CASE WHEN n.node_no > 8 AND n.node_no % 4 = 0 THEN 'AI' ELSE 'ADMIN' END,
    CASE WHEN n.node_no > 8 AND n.node_no % 4 = 0 THEN 'REVIEW_REQUIRED' ELSE 'CONFIRMED' END,
    n.node_no,
    COALESCE(CASE m.seed_idx WHEN 1 THEN 31 WHEN 7 THEN 37 WHEN 8 THEN 40 WHEN 9 THEN 43 WHEN 10 THEN 46 ELSE m.seed_idx END, 1),
    COALESCE(CASE m.seed_idx WHEN 1 THEN 31 WHEN 7 THEN 37 WHEN 8 THEN 40 WHEN 9 THEN 43 WHEN 10 THEN 46 ELSE m.seed_idx END, 1),
    0, now(), now()
FROM seed_festival_map m
JOIN festival_roadmap fr ON fr.festival_id = m.festival_id
JOIN festival_maps fm ON fm.id = fr.current_map_id
CROSS JOIN generate_series(1, 15) AS n(node_no)
CROSS JOIN LATERAL (
    SELECT
        (((n.node_no - 1) % 5) * 0.16)::numeric AS x,
        (floor((n.node_no - 1) / 5.0) * 0.22)::numeric AS y
) pos;

INSERT INTO booth_info (festival_id, roadmap_node_id, booth_name, booth_content, booth_location, created_at, updated_at)
SELECT
    m.festival_id,
    rn.id,
    rn.node_name,
    '시드 부스',
    'ZONE-' || m.seed_idx,
    now(), now()
FROM seed_festival_map m
JOIN festival_roadmap fr ON fr.festival_id = m.festival_id
JOIN roadmap_node rn
  ON rn.roadmap_id = fr.id
 AND rn.node_type = 'BOOTH'
 AND rn.review_status = 'CONFIRMED';

UPDATE roadmap_node rn
SET related_booth_id = bi.booth_id
FROM booth_info bi
WHERE bi.roadmap_node_id = rn.id;

-- ========== booth_queue (80) ==========
INSERT INTO booth_queue (
    public_id, festival_id, booth_id, tail_latitude, tail_longitude, queue_tail_meters,
    path_geometry, modifier_type, modifier_admin_id, modifier_staff_id, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    bi.festival_id,
    bi.booth_id,
    m.base_lat + (bi.booth_id % 10) * 0.00001,
    m.base_lng + (bi.booth_id % 10) * 0.00001,
    (ARRAY[0,3,5,8,10,11,12,15,18,20,22,25,28,30,31,35,40,45,50])[((bi.booth_id - 1) % 19) + 1],
    CASE WHEN bi.booth_id % 5 = 0 THEN '[{"lat":37.5665,"lng":126.9780}]'::jsonb ELSE NULL END,
    CASE WHEN bi.booth_id % 7 = 0 THEN 'ADMIN' ELSE 'STAFF' END,
    CASE WHEN bi.booth_id % 7 = 0 THEN COALESCE(CASE m.seed_idx WHEN 1 THEN 31 WHEN 7 THEN 37 WHEN 8 THEN 40 WHEN 9 THEN 43 WHEN 10 THEN 46 ELSE m.seed_idx END, 1) END,
    CASE WHEN bi.booth_id % 7 <> 0 THEN (
        SELECT fsa.id FROM field_staff_accounts fsa
        WHERE fsa.festival_id = bi.festival_id AND fsa.status = 'ACTIVE'
        ORDER BY fsa.id LIMIT 1
    ) END,
    now(), now()
FROM booth_info bi
JOIN seed_festival_map m ON m.festival_id = bi.festival_id
ORDER BY bi.booth_id
LIMIT 80;

-- ========== booth_congestion (320) ==========
INSERT INTO booth_congestion (
    booth_id, modifier_type, modifier_admin_id, modifier_staff_id,
    wait_minutes, congestion_level, created_at, updated_at
)
SELECT
    bq.booth_id,
    CASE
        WHEN g.row_no IN (3, 4) AND bq.booth_id % 3 = 0 THEN 'ADMIN'
        ELSE 'STAFF'
    END,
    CASE
        WHEN g.row_no IN (3, 4) AND bq.booth_id % 3 = 0 THEN COALESCE(
            bq.modifier_admin_id,
            CASE m.seed_idx WHEN 1 THEN 31 WHEN 7 THEN 37 WHEN 8 THEN 40 WHEN 9 THEN 43 WHEN 10 THEN 46 ELSE m.seed_idx END
        )
    END,
    CASE
        WHEN g.row_no IN (3, 4) AND bq.booth_id % 3 = 0 THEN NULL
        ELSE COALESCE(bq.modifier_staff_id, active_staff.id)
    END,
    GREATEST(0, (COALESCE(bq.queue_tail_meters, 0) / 10) * 10 + (g.row_no - 2) * 10),
    CASE
        WHEN GREATEST(0, COALESCE(bq.queue_tail_meters, 0) + (g.row_no - 2) * 5) <= 10 THEN 'LOW'
        WHEN GREATEST(0, COALESCE(bq.queue_tail_meters, 0) + (g.row_no - 2) * 5) <= 30 THEN 'MEDIUM'
        ELSE 'HIGH'
    END,
    now() - ((5 - g.row_no) * interval '45 minutes'),
    now() - ((5 - g.row_no) * interval '45 minutes')
FROM booth_queue bq
JOIN seed_festival_map m ON m.festival_id = bq.festival_id
CROSS JOIN generate_series(1, 4) AS g(row_no)
LEFT JOIN LATERAL (
    SELECT fsa.id
    FROM field_staff_accounts fsa
    WHERE fsa.festival_id = bq.festival_id
      AND fsa.status = 'ACTIVE'
    ORDER BY fsa.id
    LIMIT 1
) active_staff ON true
ORDER BY bq.booth_id, g.row_no;

-- ========== visitor counts ==========
INSERT INTO festival_visitor_count (festival_id, visit_date, visitor_count, created_at, updated_at)
SELECT
    m.festival_id,
    d::date,
    CASE
        WHEN d::date = m.start_date THEN 0
        WHEN extract(isodow FROM d) IN (6, 7) THEN 1000 + ((m.seed_idx * 131 + (d::date - m.start_date)) % 9000)
        ELSE (m.seed_idx * 37 + (d::date - m.start_date)) % 1000
    END,
    now(), now()
FROM seed_festival_map m
CROSS JOIN LATERAL generate_series(
    m.start_date,
    LEAST(COALESCE(m.end_date, CURRENT_DATE), CURRENT_DATE),
    interval '1 day'
) d
WHERE (m.progress_status = 'ongoing'
       OR (m.progress_status = 'completed' AND m.status_rank <= 2))
  AND m.start_date IS NOT NULL
  AND m.end_date IS NOT NULL;

INSERT INTO festival_visitor_total (festival_id, total_visitor_count, created_at, updated_at)
SELECT m.festival_id, 12000 + m.seed_idx * 650, now(), now()
FROM seed_festival_map m
WHERE m.progress_status = 'completed'
  AND m.status_rank > 2;
