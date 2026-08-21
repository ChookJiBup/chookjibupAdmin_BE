package com.example.chookjibupadmin.report.command.domain;

/**
 * 축제 결과 보고서 생성 작업의 진행 상태이다.
 */
public enum FestivalReportJobStatus {

    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED;

    /**
     * 아직 종료되지 않아 재생성 요청과 충돌하는 상태인지 반환한다.
     */
    public boolean isActive() {
        return this == PENDING || this == PROCESSING;
    }
}
