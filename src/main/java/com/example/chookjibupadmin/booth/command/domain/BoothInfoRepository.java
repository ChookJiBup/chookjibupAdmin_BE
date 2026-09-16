package com.example.chookjibupadmin.booth.command.domain;

import java.util.List;
import java.util.Optional;

public interface BoothInfoRepository {

    BoothInfo save(BoothInfo boothInfo);

    Optional<BoothInfo> findById(Long boothId);

    /** 계획 생성 및 현재 줄 갱신의 공통 직렬화 지점이다. */
    Optional<BoothInfo> findByIdForUpdate(Long boothId);

    Optional<BoothInfo> findByFestivalIdAndRoadmapNodeId(
            Long festivalId,
            Long roadmapNodeId
    );

    List<BoothInfo> findAllByFestivalId(Long festivalId);

    long countByFestivalId(Long festivalId);

    List<BoothInfo> findAllByRoadmapNodeIdIn(List<Long> roadmapNodeIds);

    void deleteAll(List<BoothInfo> booths);
}
