package com.example.chookjibupadmin.map.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.map.analysis.application.dto.AnalyzedMapNode;
import com.example.chookjibupadmin.map.roadmap.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MapGeometryValidatorTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private final MapGeometryValidator validator=new MapGeometryValidator();

    @Test void acceptsNormalizedRectangle() throws Exception {
        assertThat(validator.isValid(node(GeometryType.RECTANGLE,
                "{\"x\":0.1,\"y\":0.2,\"width\":0.3,\"height\":0.4,\"rotation\":0}"))).isTrue();
    }
    @Test void rejectsRectangleOutsideImage() throws Exception {
        assertThat(validator.isValid(node(GeometryType.RECTANGLE,
                "{\"x\":0.8,\"y\":0.2,\"width\":0.3,\"height\":0.4,\"rotation\":0}"))).isFalse();
    }
    @Test void rejectsPolylineWithOnePoint() throws Exception {
        assertThat(validator.isValid(node(GeometryType.POLYLINE,
                "{\"points\":[{\"x\":0.1,\"y\":0.2}]}"))).isFalse();
    }
    private AnalyzedMapNode node(GeometryType type,String json)throws Exception{return new AnalyzedMapNode(
            NodeType.BOOTH,"부스",type,mapper.readTree(json),new BigDecimal("0.9"),null);}
}
