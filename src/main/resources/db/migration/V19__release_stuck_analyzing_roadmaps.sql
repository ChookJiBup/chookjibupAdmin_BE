-- 분석이 끝나지 못한 로드맵이 ANALYZING에 갇혀 부스 편집 저장이 영구히 409로 막히던 문제를 해소한다.
-- ANALYZING을 벗어나는 경로가 분석 성공(analysisCompleted) 하나뿐이었기 때문에,
-- 분석이 실패/취소되었거나 워커가 돌지 않던 시기에 큐에 들어간 로드맵은 그대로 잠겨 있었다.
DO $$
BEGIN
    IF to_regclass('festival_roadmap') IS NULL THEN
        RETURN;
    END IF;

    UPDATE festival_roadmap r
    SET status = 'EDITING'
    WHERE r.status = 'ANALYZING'
      AND NOT EXISTS (
          -- 아직 진행 중인 분석 작업이 남아 있으면 건드리지 않는다.
          SELECT 1
          FROM map_analysis_job j
          WHERE j.map_id = r.current_map_id
            AND j.status IN ('PENDING', 'PROCESSING')
      );
END $$;
