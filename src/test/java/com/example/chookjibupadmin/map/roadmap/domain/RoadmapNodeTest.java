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

    @Test
    @DisplayName("관리자가 schema 2.0 노드를 생성하면 버전을 기록한다")
    void success_Admin_SchemaTwo() {
        RoadmapNode node = RoadmapNode.admin(
                20L, 10L, NodeType.BOOTH, "부스",
                GeometryType.POINT,
                "{\"lat\":37.5665,\"lng\":126.9780}", 1, 2L, "2.0"
        );

        assertThat(node.getGeometrySchemaVersion()).isEqualTo("2.0");
    }

    @Test
    @DisplayName("관리자가 새 노드를 생성하면 확인 완료 상태로 저장한다")
    void success_Admin() {
        RoadmapNode node = RoadmapNode.admin(
                20L, 10L, NodeType.STAGE, "무대",
                GeometryType.RECTANGLE,
                "{\"x\":0.1,\"y\":0.2}", 1, 2L
        );

        assertThat(node.getSource()).isEqualTo(NodeSource.ADMIN);
        assertThat(node.getReviewStatus()).isEqualTo(NodeReviewStatus.CONFIRMED);
        assertThat(node.getCreatedByAdminId()).isEqualTo(2L);
        assertThat(node.getLastModifiedByAdminId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("관리자가 AI 노드를 수정하면 원본 출처를 유지하고 확인 완료로 전환한다")
    void success_UpdateByAdmin() {
        RoadmapNode node = RoadmapNode.ai(
                20L, 10L, 30L, NodeType.BOOTH, "부스",
                GeometryType.POINT, "{\"x\":0.1,\"y\":0.2}",
                new BigDecimal("0.9000"), "부스", 0
        );

        node.updateByAdmin(
                NodeType.INFORMATION,
                "안내소",
                GeometryType.POINT,
                "{\"x\":0.3,\"y\":0.4}",
                2,
                3L
        );

        assertThat(node.getNodeType()).isEqualTo(NodeType.INFORMATION);
        assertThat(node.getNodeName()).isEqualTo("안내소");
        assertThat(node.getSource()).isEqualTo(NodeSource.AI);
        assertThat(node.getReviewStatus()).isEqualTo(NodeReviewStatus.CONFIRMED);
        assertThat(node.getLastModifiedByAdminId()).isEqualTo(3L);
    }
}
