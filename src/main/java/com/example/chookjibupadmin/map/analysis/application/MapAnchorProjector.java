package com.example.chookjibupadmin.map.analysis.application;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.domain.vo.MapImageAnchor;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 이미지 정규화 좌표(schema 1.0)를 앵커 기준 WGS84(schema 2.0)로 투영한다.
 *
 * <p>AI 분석은 부스를 영역 도형(RECTANGLE/POLYGON)으로 내놓지만 운영 대시보드와 편집 화면은
 * 위경도 POINT만 그린다. 그래서 영역은 중심점 POINT로 축약한다 — 이 축약을 빼먹으면
 * 파이프라인이 끝까지 돌고도 화면에는 부스가 하나도 뜨지 않는다. 대기열(POLYLINE)만은
 * 선이라는 성질이 의미를 가지므로 각 꼭짓점을 그대로 투영해 선으로 남긴다.</p>
 */
@Component
public class MapAnchorProjector {

    /** 위도 1도가 덮는 거리(m). 지구를 구로 근사한 값으로, 축제 부지 규모에서는 오차가 무시된다. */
    static final double METERS_PER_DEGREE_LATITUDE = 111_320d;

    /** 투영 결과. 축약이 일어나면 geometryType이 입력과 달라진다. */
    public record ProjectedGeometry(GeometryType geometryType, ObjectNode geometry) {
    }

    /** 표시용 오버레이 네 귀퉁이 위경도. */
    public record ProjectedCorners(
            GeoPoint topLeft,
            GeoPoint topRight,
            GeoPoint bottomRight,
            GeoPoint bottomLeft
    ) {
    }

    public record GeoPoint(BigDecimal lat, BigDecimal lng) {
    }

    /**
     * 정규화 geometry를 위경도 geometry로 옮긴다. 입력이 정규화 좌표로 읽히지 않으면 null.
     *
     * @param anchor      이미지 중심 위경도·실폭·방위각
     * @param imageWidth  분석 이미지 가로 픽셀 수
     * @param imageHeight 분석 이미지 세로 픽셀 수
     */
    public ProjectedGeometry project(
            MapImageAnchor anchor,
            int imageWidth,
            int imageHeight,
            GeometryType geometryType,
            JsonNode geometry
    ) {
        if (anchor == null || geometryType == null || geometry == null
                || !geometry.isObject() || imageWidth <= 0 || imageHeight <= 0) {
            return null;
        }

        Projection projection = new Projection(anchor, imageWidth, imageHeight);

        if (geometryType == GeometryType.POLYLINE) {
            List<NormalizedPoint> points = readPoints(geometry);
            if (points == null || points.size() < 2) {
                return null;
            }
            return new ProjectedGeometry(
                    GeometryType.POLYLINE,
                    lineNode(projection, points)
            );
        }

        NormalizedPoint center = centerOf(geometryType, geometry);
        if (center == null) {
            return null;
        }
        return new ProjectedGeometry(
                GeometryType.POINT,
                projection.toPointNode(center)
        );
    }

    /**
     * 정규화 이미지 네 귀퉁이 (0,0),(1,0),(1,1),(0,1)를 WGS84로 투영한다.
     * 극점 근처처럼 경도 환산이 불안정하면 클램프하지 않고 거절한다.
     */
    public ProjectedCorners corners(
            MapImageAnchor anchor,
            int imageWidth,
            int imageHeight
    ) {
        if (anchor == null || imageWidth <= 0 || imageHeight <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        Projection projection = new Projection(anchor, imageWidth, imageHeight);
        if (Math.abs(projection.metersPerDegreeLongitude) < 1e-9) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return new ProjectedCorners(
                projection.toGeoPoint(new NormalizedPoint(0, 0)),
                projection.toGeoPoint(new NormalizedPoint(1, 0)),
                projection.toGeoPoint(new NormalizedPoint(1, 1)),
                projection.toGeoPoint(new NormalizedPoint(0, 1))
        );
    }

    /** 도형의 대표 중심을 정규화 좌표로 구한다. */
    private NormalizedPoint centerOf(GeometryType geometryType, JsonNode geometry) {
        return switch (geometryType) {
            case POINT -> readPoint(geometry);
            case RECTANGLE -> rectangleCenter(geometry);
            case POLYGON -> polygonCenter(geometry);
            case POLYLINE -> null;
        };
    }

    private NormalizedPoint rectangleCenter(JsonNode geometry) {
        NormalizedPoint origin = readPoint(geometry);
        Double width = readNumber(geometry.get("width"));
        Double height = readNumber(geometry.get("height"));
        if (origin == null || width == null || height == null) {
            return null;
        }
        return new NormalizedPoint(origin.x() + width / 2, origin.y() + height / 2);
    }

    private NormalizedPoint polygonCenter(JsonNode geometry) {
        List<NormalizedPoint> points = readPoints(geometry);
        if (points == null || points.isEmpty()) {
            return null;
        }
        double x = 0;
        double y = 0;
        for (NormalizedPoint point : points) {
            x += point.x();
            y += point.y();
        }
        return new NormalizedPoint(x / points.size(), y / points.size());
    }

    private List<NormalizedPoint> readPoints(JsonNode geometry) {
        JsonNode points = geometry.get("points");
        if (points == null || !points.isArray()) {
            return null;
        }
        List<NormalizedPoint> parsed = new ArrayList<>();
        for (JsonNode point : points) {
            NormalizedPoint value = readPoint(point);
            if (value == null) {
                return null;
            }
            parsed.add(value);
        }
        return parsed;
    }

    private NormalizedPoint readPoint(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        Double x = readNumber(node.get("x"));
        Double y = readNumber(node.get("y"));
        if (x == null || y == null) {
            return null;
        }
        return new NormalizedPoint(x, y);
    }

    private Double readNumber(JsonNode node) {
        if (node == null || !node.isNumber() || !Double.isFinite(node.doubleValue())) {
            return null;
        }
        return node.doubleValue();
    }

    private ObjectNode lineNode(Projection projection, List<NormalizedPoint> points) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        ArrayNode array = node.putArray("points");
        for (NormalizedPoint point : points) {
            array.add(projection.toPointNode(point));
        }
        return node;
    }

    private record NormalizedPoint(double x, double y) {
    }

    /** 앵커와 이미지 종횡비로 정해지는 하나의 좌표 변환. */
    static final class Projection {

        private final double centerLatitude;
        private final double centerLongitude;
        private final double groundWidthMeters;
        private final double groundHeightMeters;
        private final double cos;
        private final double sin;
        final double metersPerDegreeLongitude;

        Projection(MapImageAnchor anchor, int imageWidth, int imageHeight) {
            centerLatitude = anchor.getCenterLatitude().doubleValue();
            centerLongitude = anchor.getCenterLongitude().doubleValue();
            groundWidthMeters = anchor.getGroundWidthMeters().doubleValue();
            groundHeightMeters = groundWidthMeters
                    * ((double) imageHeight / (double) imageWidth);
            double theta = Math.toRadians(anchor.getRotationDegrees().doubleValue());
            cos = Math.cos(theta);
            sin = Math.sin(theta);
            metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE
                    * Math.cos(Math.toRadians(centerLatitude));
        }

        private ObjectNode toPointNode(NormalizedPoint point) {
            double[] latLng = projectMeters(point);
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            node.put("lat", clamp(latLng[0], 90));
            node.put("lng", clamp(latLng[1], 180));
            return node;
        }

        GeoPoint toGeoPoint(NormalizedPoint point) {
            double[] latLng = projectMeters(point);
            if (Math.abs(latLng[0]) > 90 || Math.abs(latLng[1]) > 180
                    || !Double.isFinite(latLng[0]) || !Double.isFinite(latLng[1])) {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }
            return new GeoPoint(
                    BigDecimal.valueOf(latLng[0]).setScale(7, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(latLng[1]).setScale(7, RoundingMode.HALF_UP)
            );
        }

        private double[] projectMeters(NormalizedPoint point) {
            double dx = (point.x() - 0.5) * groundWidthMeters;
            // 이미지 y축은 아래로 자라지만 위도는 위로 자라므로 부호를 뒤집는다.
            double dy = -(point.y() - 0.5) * groundHeightMeters;
            double east = dx * cos + dy * sin;
            double north = -dx * sin + dy * cos;

            double latitude = centerLatitude + north / METERS_PER_DEGREE_LATITUDE;
            double longitude = Math.abs(metersPerDegreeLongitude) < 1e-9
                    ? centerLongitude
                    : centerLongitude + east / metersPerDegreeLongitude;
            return new double[]{latitude, longitude};
        }

        private double clamp(double value, double limit) {
            return Math.max(-limit, Math.min(limit, value));
        }
    }
}
