package com.example.chookjibupadmin.booth.command.domain;

import java.util.List;
import java.util.Optional;

public interface BoothInfoRepository {

    BoothInfo save(BoothInfo boothInfo);

    Optional<BoothInfo> findById(Long boothId);

    Optional<BoothInfo> findByFestivalIdAndRoadmapNodeId(
            Long festivalId,
            Long roadmapNodeId
    );

    List<BoothInfo> findAllByFestivalId(Long festivalId);

    long countByFestivalId(Long festivalId);
}