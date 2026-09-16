package com.example.chookjibupadmin.booth.command.domain;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** WGS84 대기 동선의 검증·길이 계산과 동선 위 줄끝 투영을 담당한다. */
public final class QueueGeometry {
    private static final double EARTH_RADIUS = 6_371_000;
    public static final double MAX_LENGTH_METERS = 5_000;
    private QueueGeometry() {}

    public static Map<String, BigDecimal> point(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null || lat.doubleValue() < 33 || lat.doubleValue() > 38.7
                || lng.doubleValue() < 124.5 || lng.doubleValue() > 132) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
        }
        return Map.of("lat", lat.setScale(7, java.math.RoundingMode.HALF_UP),
                "lng", lng.setScale(7, java.math.RoundingMode.HALF_UP));
    }

    public static List<Map<String, BigDecimal>> validate(List<Map<String, BigDecimal>> path) {
        if (path == null || path.size() < 2 || path.size() > 500) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
        }
        List<Map<String, BigDecimal>> copy = new ArrayList<>();
        for (var p : path) {
            if (p == null) throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
            var valid = point(p.get("lat"), p.get("lng"));
            if (!copy.isEmpty() && distance(copy.getLast(), valid) < 0.01) {
                throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
            }
            copy.add(valid);
        }
        if (length(copy) > MAX_LENGTH_METERS) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
        }
        QueuePlanConstraints.rejectSelfIntersection(copy);
        return List.copyOf(copy);
    }

    public static double distance(Map<String, BigDecimal> a, Map<String, BigDecimal> b) {
        double lat1 = Math.toRadians(a.get("lat").doubleValue());
        double lat2 = Math.toRadians(b.get("lat").doubleValue());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(b.get("lng").doubleValue() - a.get("lng").doubleValue());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * EARTH_RADIUS * Math.asin(Math.sqrt(Math.min(1, h)));
    }

    public static double length(List<Map<String, BigDecimal>> path) {
        double length = 0;
        for (int i = 1; i < path.size(); i++) length += distance(path.get(i - 1), path.get(i));
        return length;
    }

    /** 줄끝과 가장 가까운 구간에 투영한다. 동선 밖 10m 초과는 현장 경로 입력이 필요하다. */
    public static List<Map<String, BigDecimal>> occupiedPath(
            List<Map<String, BigDecimal>> plan, Map<String, BigDecimal> tail) {
        double nearest = Double.MAX_VALUE;
        int segment = -1;
        Map<String, BigDecimal> projected = null;
        double scale = Math.cos(Math.toRadians(tail.get("lat").doubleValue()));
        for (int i = 1; i < plan.size(); i++) {
            var a = plan.get(i - 1);
            var b = plan.get(i);
            double dx = (b.get("lng").doubleValue() - a.get("lng").doubleValue()) * scale;
            double dy = b.get("lat").doubleValue() - a.get("lat").doubleValue();
            double tx = (tail.get("lng").doubleValue() - a.get("lng").doubleValue()) * scale;
            double ty = tail.get("lat").doubleValue() - a.get("lat").doubleValue();
            double t = Math.max(0, Math.min(1, (tx * dx + ty * dy) / (dx * dx + dy * dy)));
            var p = point(BigDecimal.valueOf(a.get("lat").doubleValue() + dy * t),
                    BigDecimal.valueOf(a.get("lng").doubleValue() + dx * t / scale));
            double distance = distance(p, tail);
            if (distance < nearest) { nearest = distance; segment = i; projected = p; }
        }
        if (nearest > 10 || projected == null) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
        }
        List<Map<String, BigDecimal>> occupied = new ArrayList<>(plan.subList(0, segment));
        if (distance(occupied.getLast(), projected) >= 0.01) occupied.add(projected);
        return List.copyOf(occupied);
    }
}
