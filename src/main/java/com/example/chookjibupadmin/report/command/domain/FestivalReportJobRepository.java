package com.example.chookjibupadmin.report.command.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 축제 결과 보고서 생성 작업 저장 계약이다.
 */
public interface FestivalReportJobRepository {

    FestivalReportJob save(FestivalReportJob job);

    Optional<FestivalReportJob> findFirstPending();

    /**
     * 기준 시각보다 먼저 시작되어 아직 처리 중인 작업을 조회한다.
     */
    List<FestivalReportJob> findProcessingStartedBefore(
            LocalDateTime startedBefore,
            int limit
    );

    Optional<FestivalReportJob> findByPublicId(UUID publicId);

    Optional<FestivalReportJob> findLatestByFestivalId(Long festivalId);

    boolean existsActiveByFestivalId(Long festivalId);

    void cancelActiveByFestivalId(Long festivalId);
}
