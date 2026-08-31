package com.example.chookjibupadmin.map.roadmap.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoadmapZoneTest {

    @Test
    @DisplayName("저장된 부스 노드 목록의 null 항목을 제거한다")
    void success_Create_FiltersNullBoothNodeIds() {
        // given
        UUID boothNodeId = UUID.randomUUID();
        List<UUID> boothNodeIds = new ArrayList<>();
        boothNodeIds.add(boothNodeId);
        boothNodeIds.add(null);

        // when
        RoadmapZone zone = new RoadmapZone(
                UUID.randomUUID(),
                "판매 구역",
                0,
                boothNodeIds
        );

        // then
        assertThat(zone.boothNodeIds()).containsExactly(boothNodeId);
    }

    @Test
    @DisplayName("부스 노드 목록이 null이면 빈 목록으로 정규화한다")
    void success_Create_NormalizesNullBoothNodeIds() {
        // when
        RoadmapZone zone = new RoadmapZone(
                UUID.randomUUID(),
                "판매 구역",
                0,
                null
        );

        // then
        assertThat(zone.boothNodeIds()).isEmpty();
    }
}
