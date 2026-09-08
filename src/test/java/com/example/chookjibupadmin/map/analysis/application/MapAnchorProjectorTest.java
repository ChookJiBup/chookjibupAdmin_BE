package com.example.chookjibupadmin.map.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.example.chookjibupadmin.map.command.domain.vo.MapImageAnchor;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapAnchorProjectorTest {

    /** 위경도 1e-7도는 위도 방향으로 약 1cm다. 투영 오차 허용치로 충분히 촘촘하다. */
    private static final double TOLERANCE = 1e-7;

    private final MapAnchorProjector projector = new MapAnchorProjector();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("이미지 중심 POINT는 앵커 중심 위경도로 그대로 옮겨진다")
    void success_Project_ImageCenter() {
        MapAnchorProjector.ProjectedGeometry projected = projector.project(
                anchor("300.00", "0.000"),
                1000,
                500,
                GeometryType.POINT,
                json("{\"x\":0.5,\"y\":0.5}")
        );

        assertThat(projected).isNotNull();
        assertThat(projected.geometryType()).isEqualTo(GeometryType.POINT);
        assertLatLng(projected.geometry(), 37.0, 127.0);
    }

    @Test
    @DisplayName("이미지 좌상단은 앵커 중심의 북서쪽으로, 우하단은 남동쪽으로 간다")
    void success_Project_Corners() {
        MapImageAnchor anchor = anchor("300.00", "0.000");

        MapAnchorProjector.ProjectedGeometry topLeft = projector.project(
                anchor, 1000, 500, GeometryType.POINT, json("{\"x\":0,\"y\":0}")
        );
        MapAnchorProjector.ProjectedGeometry bottomRight = projector.project(
                anchor, 1000, 500, GeometryType.POINT, json("{\"x\":1,\"y\":1}")
        );

        // 가로 300m, 종횡비 0.5이므로 세로는 150m. 중심에서 ±150m 동서, ±75m 남북.
        assertLatLng(topLeft.geometry(), 37.0006737334, 126.9983127888);
        assertLatLng(bottomRight.geometry(), 36.9993262666, 127.0016872112);
    }

    @Test
    @DisplayName("corners는 정규화 네 귀퉁이를 WGS84로 투영한다")
    void success_Corners_NormalizedImageBounds() {
        MapAnchorProjector.ProjectedCorners corners = projector.corners(
                anchor("300.00", "0.000"),
                1000,
                500
        );

        assertThat(corners.topLeft().lat().doubleValue())
                .isCloseTo(37.0006737334, within(TOLERANCE));
        assertThat(corners.topLeft().lng().doubleValue())
                .isCloseTo(126.9983127888, within(TOLERANCE));
        assertThat(corners.bottomRight().lat().doubleValue())
                .isCloseTo(36.9993262666, within(TOLERANCE));
        assertThat(corners.bottomRight().lng().doubleValue())
                .isCloseTo(127.0016872112, within(TOLERANCE));
    }

    @Test
    @DisplayName("회전 90도면 이미지 위쪽이 동쪽을 가리킨다")
    void success_Project_Rotated90Degrees() {
        MapAnchorProjector.ProjectedGeometry projected = projector.project(
                anchor("200.00", "90.000"),
                1000,
                1000,
                GeometryType.POINT,
                json("{\"x\":0.5,\"y\":0}")
        );

        // 이미지 위쪽 끝(중심에서 100m)이 정동쪽으로 100m 이동한다 — 위도는 그대로다.
        assertLatLng(projected.geometry(), 37.0, 127.0011248075);
    }

    @Test
    @DisplayName("세로로 긴 이미지는 종횡비만큼 남북 방향 실거리가 늘어난다")
    void success_Project_NonSquareAspect() {
        MapAnchorProjector.ProjectedGeometry projected = projector.project(
                anchor("100.00", "0.000"),
                1000,
                2000,
                GeometryType.POINT,
                json("{\"x\":0.5,\"y\":0}")
        );

        // 가로 100m, 종횡비 2이므로 세로는 200m. 위쪽 끝은 중심에서 북쪽으로 100m다.
        assertLatLng(projected.geometry(), 37.0008983112, 127.0);
    }

    @Test
    @DisplayName("RECTANGLE은 중심점 POINT로 축약된다")
    void success_Project_RectangleToCenterPoint() {
        MapAnchorProjector.ProjectedGeometry projected = projector.project(
                anchor("300.00", "0.000"),
                1000,
                500,
                GeometryType.RECTANGLE,
                json("{\"x\":0.25,\"y\":0.25,\"width\":0.5,\"height\":0.5,\"rotation\":0}")
        );

        assertThat(projected.geometryType()).isEqualTo(GeometryType.POINT);
        assertLatLng(projected.geometry(), 37.0, 127.0);
    }

    @Test
    @DisplayName("POLYGON은 꼭짓점 평균 위치의 POINT로 축약된다")
    void success_Project_PolygonToCenterPoint() {
        MapAnchorProjector.ProjectedGeometry projected = projector.project(
                anchor("300.00", "0.000"),
                1000,
                500,
                GeometryType.POLYGON,
                json("{\"points\":[{\"x\":0,\"y\":0},{\"x\":1,\"y\":0},"
                        + "{\"x\":1,\"y\":1},{\"x\":0,\"y\":1}]}")
        );

        assertThat(projected.geometryType()).isEqualTo(GeometryType.POINT);
        assertLatLng(projected.geometry(), 37.0, 127.0);
    }

    @Test
    @DisplayName("POLYLINE은 선의 의미가 있으므로 꼭짓점을 그대로 투영해 선으로 남는다")
    void success_Project_PolylineKeepsPoints() {
        MapAnchorProjector.ProjectedGeometry projected = projector.project(
                anchor("300.00", "0.000"),
                1000,
                500,
                GeometryType.POLYLINE,
                json("{\"points\":[{\"x\":0,\"y\":0},{\"x\":0.5,\"y\":0.5}]}")
        );

        assertThat(projected.geometryType()).isEqualTo(GeometryType.POLYLINE);
        JsonNode points = projected.geometry().get("points");
        assertThat(points).hasSize(2);
        assertLatLng(points.get(0), 37.0006737334, 126.9983127888);
        assertLatLng(points.get(1), 37.0, 127.0);
    }

    @Test
    @DisplayName("앵커가 없거나 이미지 크기를 모르면 투영하지 않는다")
    void fail_Project_MissingAnchorOrDimensions() {
        assertThat(projector.project(
                null, 1000, 500, GeometryType.POINT, json("{\"x\":0.5,\"y\":0.5}")
        )).isNull();
        assertThat(projector.project(
                anchor("300.00", "0.000"), 0, 500,
                GeometryType.POINT, json("{\"x\":0.5,\"y\":0.5}")
        )).isNull();
    }

    @Test
    @DisplayName("정규화 좌표를 읽을 수 없는 geometry는 투영하지 않는다")
    void fail_Project_MalformedGeometry() {
        assertThat(projector.project(
                anchor("300.00", "0.000"), 1000, 500,
                GeometryType.POINT, json("{\"lat\":37,\"lng\":127}")
        )).isNull();
        assertThat(projector.project(
                anchor("300.00", "0.000"), 1000, 500,
                GeometryType.POLYLINE, json("{\"points\":[{\"x\":0,\"y\":0}]}")
        )).isNull();
    }

    private MapImageAnchor anchor(String groundWidthMeters, String rotationDegrees) {
        return MapImageAnchor.of(
                new BigDecimal("37.0000000"),
                new BigDecimal("127.0000000"),
                new BigDecimal(groundWidthMeters),
                new BigDecimal(rotationDegrees)
        );
    }

    private void assertLatLng(JsonNode node, double latitude, double longitude) {
        assertThat(node.get("lat").doubleValue())
                .isCloseTo(latitude, within(TOLERANCE));
        assertThat(node.get("lng").doubleValue())
                .isCloseTo(longitude, within(TOLERANCE));
    }

    private JsonNode json(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
