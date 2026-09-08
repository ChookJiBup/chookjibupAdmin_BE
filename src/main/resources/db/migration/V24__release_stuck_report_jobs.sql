-- 결과 보고서 생성이 «분석 중»에서 넘어가지 못하고 PROCESSING에 갇히던 문제를 해소한다.
-- 워커가 detached 상태의 작업을 반복 병합하다 낙관적 락 예외를 만나면
-- 실패 기록조차 남기지 못한 채 예외가 스케줄러 밖으로 빠져나갔고,
-- PENDING만 다시 선택되므로 해당 작업은 영구히 처리 중으로 남았다.
DO $$
BEGIN
    IF to_regclass('festival_report_job') IS NULL THEN
        RETURN;
    END IF;

    -- 아직 재시도 여유가 있는 작업은 대기로 되돌려 워커가 다시 집도록 한다.
    UPDATE festival_report_job
    SET status = 'PENDING',
        next_attempt_at = now(),
        failure_code = 'REPORT_ANALYSIS_TIMEOUT',
        failure_message = '결과 보고서 분석이 제한 시간 안에 끝나지 않았습니다.',
        updated_at = now()
    WHERE status = 'PROCESSING'
      AND attempt_count < 3;

    -- 재시도 한도를 넘긴 작업은 사유를 남기고 실패로 종료한다.
    UPDATE festival_report_job
    SET status = 'FAILED',
        completed_at = now(),
        next_attempt_at = NULL,
        failure_code = 'REPORT_ANALYSIS_TIMEOUT',
        failure_message = '결과 보고서 분석이 제한 시간 안에 끝나지 않았습니다.',
        updated_at = now()
    WHERE status = 'PROCESSING'
      AND attempt_count >= 3;
END $$;
