package com.example.chookjibupadmin.festival.command.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * 연도별 개최 축제를 묶는 축제 시리즈 저장소 계약이다.
 */
public interface FestivalSeriesRepository {

    /**
     * 축제 시리즈를 저장한다.
     */
    FestivalSeries save(FestivalSeries festivalSeries);

    /**
     * 축제 시리즈 ID로 조회한다.
     */
    Optional<FestivalSeries> findById(Long seriesId);

    /**
     * 외부 노출용 축제 시리즈 UUID로 조회한다.
     */
    Optional<FestivalSeries> findByPublicId(UUID publicId);

    /**
     * 정규화된 축제명으로 기존 축제 시리즈를 조회한다.
     */
    Optional<FestivalSeries> findByNormalizedName(String normalizedName);
}
