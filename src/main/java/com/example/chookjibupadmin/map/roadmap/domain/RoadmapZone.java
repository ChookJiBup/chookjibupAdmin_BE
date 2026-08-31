package com.example.chookjibupadmin.map.roadmap.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 지도 편집기에서 관리하는 부스 구역의 영속 값이다. */
public record RoadmapZone(
        UUID zoneId,
        String name,
        int sortOrder,
        List<UUID> boothNodeIds
) {

    public RoadmapZone {
        boothNodeIds = boothNodeIds == null
                ? List.of()
                : boothNodeIds.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }
}
