package com.example.chookjibupadmin.map.analysis.application;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * WGS84 폐쇄 폴리곤 부지 경계를 검증한다.
 */
@Component
public class MapBoundaryValidator {

    private static final int MIN_UNIQUE_VERTICES = 3;
    private static final int MAX_POINTS = 500;
    private static final BigDecimal LAT_MIN = new BigDecimal("33.0");
    private static final BigDecimal LAT_MAX = new BigDecimal("38.7");
    private static final BigDecimal LNG_MIN = new BigDecimal("124.5");
    private static final BigDecimal LNG_MAX = new BigDecimal("132.0");

    public record BoundaryPoint(BigDecimal lat, BigDecimal lng) {
    }

    /**
     * 유효한 폐쇄 폴리곤이면 정규화된 점 목록(마지막=첫점 중복 제거)을 반환한다.
     */
    public List<BoundaryPoint> validateClosedPolygon(List<BoundaryPoint> rawPoints) {
        if (rawPoints == null || rawPoints.isEmpty()) {
            throw new CustomException(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID);
        }
        if (rawPoints.size() > MAX_POINTS) {
            throw new CustomException(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID);
        }

        List<BoundaryPoint> points = new ArrayList<>(rawPoints.size());
        for (BoundaryPoint point : rawPoints) {
            if (point == null || point.lat() == null || point.lng() == null) {
                throw new CustomException(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID);
            }
            validateKoreaRange(point.lat(), point.lng());
            points.add(point);
        }

        // 폐쇄 링의 마지막=첫점 중복은 제거해 고유 꼭짓점만 검증한다.
        if (points.size() >= 2 && samePoint(points.getFirst(), points.getLast())) {
            points.removeLast();
        }

        if (points.size() < MIN_UNIQUE_VERTICES) {
            throw new CustomException(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID);
        }

        for (int i = 1; i < points.size(); i++) {
            if (samePoint(points.get(i - 1), points.get(i))) {
                throw new CustomException(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID);
            }
        }

        Set<String> unique = new HashSet<>();
        for (BoundaryPoint point : points) {
            unique.add(point.lat().stripTrailingZeros().toPlainString()
                    + ","
                    + point.lng().stripTrailingZeros().toPlainString());
        }
        if (unique.size() < MIN_UNIQUE_VERTICES) {
            throw new CustomException(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID);
        }

        if (hasSelfIntersection(points) || isZeroArea(points)) {
            throw new CustomException(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID);
        }
        return List.copyOf(points);
    }

    private void validateKoreaRange(BigDecimal lat, BigDecimal lng) {
        if (lat.compareTo(LAT_MIN) < 0 || lat.compareTo(LAT_MAX) > 0
                || lng.compareTo(LNG_MIN) < 0 || lng.compareTo(LNG_MAX) > 0) {
            throw new CustomException(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID);
        }
    }

    private boolean samePoint(BoundaryPoint a, BoundaryPoint b) {
        return a.lat().compareTo(b.lat()) == 0 && a.lng().compareTo(b.lng()) == 0;
    }

    private boolean isZeroArea(List<BoundaryPoint> points) {
        double area = 0;
        int n = points.size();
        for (int i = 0; i < n; i++) {
            BoundaryPoint current = points.get(i);
            BoundaryPoint next = points.get((i + 1) % n);
            area += current.lng().doubleValue() * next.lat().doubleValue()
                    - next.lng().doubleValue() * current.lat().doubleValue();
        }
        return Math.abs(area) < 1e-18;
    }

    /**
     * 단순 O(n^2) 선분 교차 검사. 인접·공유 꼭짓점 선분은 교차로 보지 않는다.
     */
    private boolean hasSelfIntersection(List<BoundaryPoint> points) {
        int n = points.size();
        for (int i = 0; i < n; i++) {
            Point a1 = toPoint(points.get(i));
            Point a2 = toPoint(points.get((i + 1) % n));
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(i - j) <= 1 || (i == 0 && j == n - 1)) {
                    continue;
                }
                Point b1 = toPoint(points.get(j));
                Point b2 = toPoint(points.get((j + 1) % n));
                if (segmentsIntersect(a1, a2, b1, b2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Point toPoint(BoundaryPoint point) {
        return new Point(point.lng().doubleValue(), point.lat().doubleValue());
    }

    private boolean segmentsIntersect(Point p1, Point q1, Point p2, Point q2) {
        int o1 = orientation(p1, q1, p2);
        int o2 = orientation(p1, q1, q2);
        int o3 = orientation(p2, q2, p1);
        int o4 = orientation(p2, q2, q1);

        if (o1 != o2 && o3 != o4) {
            return true;
        }
        if (o1 == 0 && onSegment(p1, p2, q1)) {
            return true;
        }
        if (o2 == 0 && onSegment(p1, q2, q1)) {
            return true;
        }
        if (o3 == 0 && onSegment(p2, p1, q2)) {
            return true;
        }
        return o4 == 0 && onSegment(p2, q1, q2);
    }

    private int orientation(Point p, Point q, Point r) {
        double value = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
        if (Math.abs(value) < 1e-18) {
            return 0;
        }
        return value > 0 ? 1 : 2;
    }

    private boolean onSegment(Point p, Point q, Point r) {
        return q.x <= Math.max(p.x, r.x) + 1e-18
                && q.x + 1e-18 >= Math.min(p.x, r.x)
                && q.y <= Math.max(p.y, r.y) + 1e-18
                && q.y + 1e-18 >= Math.min(p.y, r.y);
    }

    private record Point(double x, double y) {
    }
}
