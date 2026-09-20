-- 2025 축지법 테스트 축제의 지도와 운영 대시보드에서 함께 쓸 시연 부스.
-- 해당 축제의 좌표 지도가 없거나 이미 동명 부스가 있으면 기존 데이터를 보존한다.
DO $$
DECLARE
    target_festival BIGINT;
    target_roadmap BIGINT;
    target_map BIGINT;
    next_order INTEGER;
    booth_record RECORD;
    new_node BIGINT;
    new_booth BIGINT;
    added_count INTEGER := 0;
BEGIN
    SELECT f.festival_id, r.id, r.current_map_id
      INTO target_festival, target_roadmap, target_map
      FROM festivals f
      JOIN festival_roadmap r ON r.festival_id = f.festival_id
      JOIN festival_maps m ON m.id = r.current_map_id
     WHERE f.festival_name IN ('2025 축지법테스트', '2025 축지법 테스트')
       AND f.festival_year = 2025
       AND m.map_kind = 'COORDINATE'
     ORDER BY f.festival_id
     LIMIT 1;

    IF target_festival IS NULL THEN
        RAISE NOTICE '2025 축지법 테스트 좌표 지도가 없어 시연 부스를 추가하지 않았습니다';
        RETURN;
    END IF;

    SELECT COALESCE(MAX(sort_order), -1) + 1 INTO next_order
      FROM roadmap_node WHERE roadmap_id = target_roadmap AND map_id = target_map;

    FOR booth_record IN
        SELECT * FROM (VALUES
            ('종합안내·분실물', 37.520934, 127.122959, '행사 안내와 분실물 접수'),
            ('지역 관광 안내', 37.520990, 127.122850, '송파 관광지와 이동 경로 안내'),
            ('체험 프로그램 접수', 37.520990, 127.123070, '현장 체험 예약과 참가 확인'),
            ('지역 먹거리', 37.520870, 127.122840, '지역 음식 판매'),
            ('음료·휴식', 37.520870, 127.123070, '음료 구매와 휴식'),
            ('의료·안전 지원', 37.521045, 127.122960, '응급 처치와 안전 문의')
        ) AS v(name, latitude, longitude, description)
    LOOP
        IF EXISTS (
            SELECT 1 FROM roadmap_node
             WHERE roadmap_id = target_roadmap AND map_id = target_map
               AND node_type = 'BOOTH' AND node_name = booth_record.name
        ) THEN
            CONTINUE;
        END IF;

        INSERT INTO roadmap_node (
            public_id, roadmap_id, map_id, node_type, node_name,
            geometry_type, geometry_data, geometry_schema_version,
            source, review_status, sort_order, created_at, updated_at, version
        ) VALUES (
            gen_random_uuid(), target_roadmap, target_map, 'BOOTH', booth_record.name,
            'POINT', jsonb_build_object('lat', booth_record.latitude, 'lng', booth_record.longitude), '2.0',
            'ADMIN', 'CONFIRMED', next_order, clock_timestamp(), clock_timestamp(), 0
        ) RETURNING id INTO new_node;

        INSERT INTO booth_info (
            festival_id, roadmap_node_id, booth_name, booth_content,
            created_at, updated_at
        ) VALUES (
            target_festival, new_node, booth_record.name, booth_record.description,
            clock_timestamp(), clock_timestamp()
        ) RETURNING booth_id INTO new_booth;

        UPDATE roadmap_node SET related_booth_id = new_booth WHERE id = new_node;
        INSERT INTO booth_queue (public_id, festival_id, booth_id)
        VALUES (gen_random_uuid(), target_festival, new_booth);
        next_order := next_order + 1;
        added_count := added_count + 1;
    END LOOP;

    UPDATE festival_roadmap SET edit_revision = edit_revision + 1,
        published_version = CASE WHEN status = 'PUBLISHED' THEN edit_revision + 1 ELSE published_version END,
        updated_at = clock_timestamp()
    WHERE id = target_roadmap AND added_count > 0;
END $$;
