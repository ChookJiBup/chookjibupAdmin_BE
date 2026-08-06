package com.example.chookjibupadmin.map.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.map.analysis.application.dto.AnalyzedMapNode;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.map.roadmap.domain.NodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapGeometryValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final MapGeometryValidator validator =
            new MapGeometryValidator();

    @Test
    @DisplayName("이미지 안의 정규화된 사각형을 허용한다")
    void success_IsValid_NormalizedRectangle() throws Exception {
        assertThat(validator.isValid(node(
                GeometryType.RECTANGLE,
                """
                {"x":0.1,"y":0.2,"width":0.3,"height":0.4,"rotation":0}
                """
        ))).isTrue();
    }

    @Test
    @DisplayName("이미지 경계를 벗어난 사각형을 거부한다")
    void fail_IsValid_RectangleOutsideImage() throws Exception {
        assertThat(validator.isValid(node(
                GeometryType.RECTANGLE,
                """
                {"x":0.8,"y":0.2,"width":0.3,"height":0.4,"rotation":0}
                """
        ))).isFalse();
    }

    @Test
    @DisplayName("점이 하나뿐인 선을 거부한다")
    void fail_IsValid_PolylineWithOnePoint() throws Exception {
        assertThat(validator.isValid(node(
                GeometryType.POLYLINE,
                """
                {"points":[{"x":0.1,"y":0.2}]}
                """
        ))).isFalse();
    }

    private AnalyzedMapNode node(
            GeometryType type,
            String json
    ) throws Exception {
        return new AnalyzedMapNode(
                NodeType.BOOTH,
                "부스",
                type,
                mapper.readTree(json),
                new BigDecimal("0.9"),
                null
        );
    }
}
