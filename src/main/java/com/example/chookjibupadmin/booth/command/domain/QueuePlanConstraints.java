package com.example.chookjibupadmin.booth.command.domain;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 경계/시설/다른 동선과의 교차를 검사한다. 경계가 없으면 검증 완료로 취급하지 않는다. */
public final class QueuePlanConstraints {
    private QueuePlanConstraints() {}
    public record Obstacle(List<Map<String, BigDecimal>> points, boolean polygon) {}

    public static void rejectSelfIntersection(List<Map<String, BigDecimal>> path) {
        // 인접 선분은 끝점을 공유하지만, 같은 선을 되짚으면 줄 길이가 중복 계산된다.
        for (int i = 2; i < path.size(); i++) {
            var a = path.get(i - 2);
            var b = path.get(i - 1);
            var c = path.get(i);
            if (Math.abs(turn(a, b, c)) < 1e-16
                    && (x(b) - x(a)) * (x(c) - x(b)) + (y(b) - y(a)) * (y(c) - y(b)) < 0) invalid();
        }
        for (int i = 1; i < path.size(); i++) {
            for (int j = i + 2; j < path.size(); j++) {
                if (i == 1 && j == path.size() - 1
                        && QueueGeometry.distance(path.getFirst(),path.getLast()) < 0.01) continue;
                if (crosses(path.get(i-1),path.get(i),List.of(path.get(j-1),path.get(j)),false)) invalid();
            }
        }
    }

    public static void validate(List<Map<String, BigDecimal>> path,
            List<Map<String, BigDecimal>> boundary, List<Obstacle> obstacles) {
        if (boundary != null) {
            for (var point : path) if (!inside(point, boundary)) invalid();
            for (int i = 1; i < path.size(); i++) {
                // 꼭짓점이 모두 안쪽이어도 오목한 경계를 가로지르는 경로는 차단한다.
                if (crosses(path.get(i - 1), path.get(i), boundary, true)) invalid();
            }
        }
        for (var obstacle : obstacles) {
            if (obstacle.polygon() && path.stream().anyMatch(p -> inside(p, obstacle.points()))) invalid();
            for (int i = 1; i < path.size(); i++) {
                if (obstacle.points().size() == 1) {
                    if (nearSegment(path.get(i - 1), path.get(i), obstacle.points().getFirst())) invalid();
                } else if (crosses(path.get(i - 1), path.get(i), obstacle.points(), obstacle.polygon())) invalid();
            }
        }
    }

    private static boolean nearSegment(Map<String, BigDecimal> a, Map<String, BigDecimal> b,
            Map<String, BigDecimal> p) {
        double scale = Math.cos(Math.toRadians(p.get("lat").doubleValue()));
        double dx = (x(b) - x(a)) * scale, dy = y(b) - y(a);
        double t = Math.max(0, Math.min(1, ((x(p) - x(a)) * scale * dx + (y(p) - y(a)) * dy) / (dx * dx + dy * dy)));
        var closest = QueueGeometry.point(BigDecimal.valueOf(y(a) + t * dy), BigDecimal.valueOf(x(a) + t * dx / scale));
        return QueueGeometry.distance(closest, p) < 2;
    }
    private static boolean crosses(Map<String, BigDecimal> a, Map<String, BigDecimal> b,
            List<Map<String, BigDecimal>> points, boolean close) {
        int edges = close ? points.size() : points.size() - 1;
        for (int i = 0; i < edges; i++) {
            var c = points.get(i);
            var d = points.get((i + 1) % points.size());
            if (turn(a,b,c) * turn(a,b,d) < 0 && turn(c,d,a) * turn(c,d,b) < 0) return true;
            if (onSegment(a,b,c) || onSegment(a,b,d) || onSegment(c,d,a) || onSegment(c,d,b)) return true;
        }
        return false;
    }
    private static boolean inside(Map<String, BigDecimal> p, List<Map<String, BigDecimal>> polygon) {
        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            var a = polygon.get(i);
            var b = polygon.get(j);
            if ((y(a) > y(p)) != (y(b) > y(p))
                    && x(p) < (x(b)-x(a)) * (y(p)-y(a)) / (y(b)-y(a)) + x(a)) inside = !inside;
        }
        return inside;
    }
    private static double turn(Map<String, BigDecimal> a, Map<String, BigDecimal> b, Map<String, BigDecimal> c) {
        return (x(b)-x(a))*(y(c)-y(a))-(y(b)-y(a))*(x(c)-x(a));
    }
    private static boolean onSegment(Map<String, BigDecimal> a, Map<String, BigDecimal> b, Map<String, BigDecimal> c) {
        return Math.abs(turn(a,b,c)) < 1e-16 && x(c) >= Math.min(x(a),x(b)) - 1e-12
                && x(c) <= Math.max(x(a),x(b)) + 1e-12 && y(c) >= Math.min(y(a),y(b)) - 1e-12
                && y(c) <= Math.max(y(a),y(b)) + 1e-12;
    }
    private static double x(Map<String, BigDecimal> p) { return p.get("lng").doubleValue(); }
    private static double y(Map<String, BigDecimal> p) { return p.get("lat").doubleValue(); }
    private static void invalid() { throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID); }
}
