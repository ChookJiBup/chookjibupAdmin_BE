package com.example.chookjibupadmin.report.command.domain;

import java.util.Optional;

/**
 * 축제 결과 보고서 스냅샷 저장 계약이다.
 */
public interface FestivalResultRepository {

    FestivalResult save(FestivalResult result);

    Optional<FestivalResult> findByFestivalId(Long festivalId);
}
