package com.example.chookjibupadmin.festival.command.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * 축제 기본 정보를 저장하는 저장소 계약이다.
 */
public interface FestivalRepository {

    /**
     * 축제 기본 정보를 저장한다.
     */
    Festival save(Festival festival);

    /**
     * 축제 기본 정보를 삭제한다.
     */
    void delete(Festival festival);

    /**
     * 축제 ID로 축제 기본 정보를 조회한다.
     */
    Optional<Festival> findById(Long festivalId);

    Optional<Festival> findByIdForUpdate(Long festivalId);

    /**
     * 외부 노출용 축제 UUID로 축제 기본 정보를 조회한다.
     */
    Optional<Festival> findByPublicId(UUID publicId);

    /**
     * 외부 UUID로 축제를 잠금 조회한다.
     */
    Optional<Festival> findByPublicIdForUpdate(UUID publicId);

    /**
     * 지정한 축제 묶음의 특정 개최 연도 축제가 이미 존재하는지 확인한다.
     */
    boolean existsBySeriesIdAndYear(Long seriesId, int year);

    Optional<Festival> findBySeriesIdAndYear(Long seriesId, int year);
}
