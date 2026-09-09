-- 지금 방문객에게 실제로 보이고 있는 배치도를 PUBLISHED로 올린다.
--
-- 사용자 백엔드는 원래 «로드맵 상태가 PUBLISHED일 때만 방문객에게 부스지도를 내려준다»는
-- 규칙을 두었지만, 지금은 그 게이트가 꺼져 있어 저장만 된 배치도까지 그대로 노출된다.
-- 게이트를 그대로 되살리면 오늘까지 잘 보이던 축제들이 관리자가 아무것도 하지 않았는데
-- 한꺼번에 사라진다. 축제 기간 중이면 방문객이 지도를 못 보는 사고가 된다.
--
-- 그래서 «지금 보이고 있는 것»을 공개 상태로 미리 기록해 둔다. 조건은 사용자 백엔드가
-- 실제로 화면에 그리는 기준과 같다 — 편집 중(EDITING)이면서, 검수까지 끝난(CONFIRMED)
-- 부스(BOOTH) 노드를 현재 지도에 하나 이상 가진 로드맵. 부스가 하나도 없으면 게이트를
-- 켜든 말든 방문객 화면은 어차피 비어 있으므로 건드리지 않는다.
--
-- 이 마이그레이션이 배포된 뒤에 사용자 백엔드의 게이트 복원이 나가야 한다.
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
    WHERE r.status = 'EDITING'
      AND EXISTS (
          SELECT 1
          FROM roadmap_node n
          WHERE n.roadmap_id = r.id
            AND n.map_id = r.current_map_id
            AND n.node_type = 'BOOTH'
            AND n.review_status = 'CONFIRMED'
      );
END $$;
