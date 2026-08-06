package com.example.chookjibupadmin.map.roadmap.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoadmapNodeTest {

    @Test
    @DisplayName("AI 분석 결과를 관리자 검수 대기 노드로 생성한다")
    void success_Ai() {
        // when
        RoadmapNode node = RoadmapNode.ai(
                20L,
                10L,
                30L,
                NodeType.BOOTH,
                "부스 1",
                GeometryType.RECTANGLE,
                "{\"x\":0.1,\"y\":0.2}",
                new BigDecimal("0.9500"),
                "부스 1",
                0
        );

        // then
        assertThat(node.getPublicId()).isNotNull();
        assertThat(node.getRoadmapId()).isEqualTo(20L);
        assertThat(node.getMapId()).isEqualTo(10L);
        assertThat(node.getAnalysisJobId()).isEqualTo(30L);
        assertThat(node.getSource()).isEqualTo(NodeSource.AI);
        assertThat(node.getReviewStatus())
                .isEqualTo(NodeReviewStatus.REVIEW_REQUIRED);
        assertThat(node.getGeometrySchemaVersion()).isEqualTo("1.0");
    }
}
