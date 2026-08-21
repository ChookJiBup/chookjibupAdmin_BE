package com.example.chookjibupadmin.report.command.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * 축제 결과 보고서 생성 작업 저장 계약이다.
 */
public interface FestivalReportJobRepository {

    FestivalReportJob save(FestivalReportJob job);

    Optional<FestivalReportJob> findFirstPending();

    Optional<FestivalReportJob> findByPublicId(UUID publicId);

    Optional<FestivalReportJob> findLatestByFestivalId(Long festivalId);

    boolean existsActiveByFestivalId(Long festivalId);

    void cancelActiveByFestivalId(Long festivalId);
}
