package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.booth.command.application.dto.UpdateBoothQueueCommand;
import com.example.chookjibupadmin.booth.command.domain.*;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 경로 교체/줄끝 관측/경로 삭제의 호환 계약을 해석한다. */
public final class QueueObservationResolver {
    private QueueObservationResolver() {}
    public record Observation(BigDecimal lat, BigDecimal lng, Integer meters,
            List<Map<String, BigDecimal>> path, BoothCongestionEstimate estimate,
            Long planRevision, String method, boolean refreshObservation) {}

    public static Observation resolve(BoothQueue queue, BoothQueuePlan plan,
            Map<String, BigDecimal> boothPoint, UpdateBoothQueueCommand command) {
        var tail = QueueGeometry.point(command.tailLatitude(), command.tailLongitude());
        if (command.queueTailMeters() != null && (command.queueTailMeters() < 0
                || command.queueTailMeters() > QueueGeometry.MAX_LENGTH_METERS)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (command.planRevision() != null && (plan == null || command.planRevision() != plan.getRevision())) {
            throw new CustomException(ErrorCode.BOOTH_QUEUE_REVISION_CONFLICT);
        }
        List<Map<String, BigDecimal>> path = null;
        Integer meters = null;
        Long usedPlanRevision = plan == null ? null : plan.getRevision();
        String method = null;
        if (command.path() != null && !command.path().isEmpty()) {
            path = QueueGeometry.validate(command.path().stream().map(p -> {
                if (p == null) throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
                return QueueGeometry.point(p.lat(), p.lng());
            }).toList());
            var last = path.getLast();
            if (!samePoint(last, tail)) throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
            if (boothPoint != null && QueueGeometry.distance(boothPoint, path.getFirst()) > 10) {
                throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
            }
            meters = toMeters(QueueGeometry.length(path)); method = "PATH";
        } else if (command.path() != null && sameTail(queue, tail) && command.queueTailMeters() == null) {
            // 구 FE의 경로 삭제는 시간 관측을 바꾸지 않는다.
            meters = queue.getQueueTailMeters(); method = queue.getCalculationMethod();
            return new Observation(tail.get("lat"), tail.get("lng"), meters, null,
                    queue.getWaitMinutes() == null ? null : new BoothCongestionEstimate(
                            queue.getCongestionLevel(), queue.getWaitMinutes()), queue.getPlanRevision(), method, false);
        } else if (command.queueTailMeters() != null && command.queueTailMeters() == 0) {
            if (boothPoint != null && QueueGeometry.distance(boothPoint, tail) > 10) {
                throw new CustomException(ErrorCode.BOOTH_QUEUE_PATH_INVALID);
            }
            meters = 0; method = "EMPTY";
        } else if (plan != null && command.path() == null) {
            path = QueueGeometry.occupiedPath(plan.getPathGeometry(), tail);
            tail = path.getLast(); meters = toMeters(QueueGeometry.length(path));
            if (path.size() == 1) path = null;
            method = "PLAN"; usedPlanRevision = plan.getRevision();
        } else if (command.path() == null && sameTail(queue, tail) && queue.getPathGeometry() != null) {
            path = QueueGeometry.validate(queue.getPathGeometry());
            meters = toMeters(QueueGeometry.length(path)); method = "PATH";
        } else if (boothPoint != null) {
            meters = toMeters(QueueGeometry.distance(boothPoint, tail)); method = "STRAIGHT";
            if (meters > 0 && command.path() == null) path = List.of(boothPoint, tail);
        } else if (command.queueTailMeters() != null) {
            // 좌표 없는 기존 승인 부스의 거리 보고는 호환을 위해 유지한다.
            meters = command.queueTailMeters(); method = "REPORTED";
        }
        var settings = plan == null ? QueueEstimationSettings.defaults() : plan.getSettings();
        return new Observation(tail.get("lat"), tail.get("lng"), meters, path,
                meters == null ? null : settings.estimate(meters), usedPlanRevision, method, true);
    }

    private static boolean sameTail(BoothQueue queue, Map<String, BigDecimal> tail) {
        return queue.getTailLatitude() != null && queue.getTailLongitude() != null
                && queue.getTailLatitude().compareTo(tail.get("lat")) == 0
                && queue.getTailLongitude().compareTo(tail.get("lng")) == 0;
    }
    private static int toMeters(double length) { return length == 0 ? 0 : (int) Math.max(1,Math.round(length)); }
    private static boolean samePoint(Map<String, BigDecimal> a, Map<String, BigDecimal> b) {
        return a.get("lat").compareTo(b.get("lat")) == 0 && a.get("lng").compareTo(b.get("lng")) == 0;
    }
}
