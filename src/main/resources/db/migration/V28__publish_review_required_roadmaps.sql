-- V27이 놓친 로드맵을 마저 PUBLISHED로 올린다.
--
-- V27은 «지금 방문객에게 보이던 배치도»를 공개 상태로 미리 기록해 두려던 것이었는데,
-- 조건을 status = 'EDITING' 하나로만 잡았다. 그런데 AI 분석이 끝나면 로드맵은
-- REVIEW_REQUIRED가 되고, 관리자가 부스를 하나씩 승인(approveBooth)해도 노드만
-- CONFIRMED가 될 뿐 로드맵 상태는 REVIEW_REQUIRED로 남는다. 즉 «부스가 다 승인돼
-- 방문객에게 잘 보이고 있었는데 상태만 REVIEW_REQUIRED»인 로드맵이 존재한다.
--
-- 그 결과 사용자 백엔드의 PUBLISHED 게이트가 켜진 직후, 진행 중이던
-- 2026 경산갓바위소원성취축제(부스 8개)의 부스지도가 방문객 앱에서 사라졌다.
-- 축제 상세 API의 roadmap이 null로 내려갔고, 부스 자체는 살아 있어
-- /congestion 은 정상이었다.
--
-- 그래서 V27과 똑같은 조건에 상태 범위만 REVIEW_REQUIRED까지 넓혀 다시 한 번 올린다.
-- 이미 PUBLISHED가 된 로드맵은 조건에서 빠지므로 V27과 겹쳐도 무해하다.
--
-- ANALYZING은 올리지 않는다. 분석 중에는 AI가 현재 지도의 노드를 통째로 갈아끼우므로
-- 검수 전 배치가 방문객에게 나갈 수 있고, 도메인의 publish()도 같은 이유로 ANALYZING을
-- 거부한다(FestivalRoadmap.ensurePublishable).
DO $$
BEGIN
    IF to_regclass('festival_roadmap') IS NULL
        OR to_regclass('roadmap_node') IS NULL THEN
        RETURN;
    END IF;

    UPDATE festival_roadmap r
    SET status = 'PUBLISHED',
        -- 공개본은 방금 저장된 판이다. 관리자가 다시 저장하면 applyAdminEdit이 이어서 올린다.
        published_version = r.edit_revision
    WHERE r.status IN ('EDITING', 'REVIEW_REQUIRED')
      AND EXISTS (
          SELECT 1
          FROM roadmap_node n
          WHERE n.roadmap_id = r.id
            AND n.map_id = r.current_map_id
            AND n.node_type = 'BOOTH'
            AND n.review_status = 'CONFIRMED'
      );
END $$;
