package com.example.chookjibupadmin.dashboard.query.infrastructure;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.booth.command.application.BoothCongestionService;
import com.example.chookjibupadmin.booth.command.application.BoothInfoService;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionModifierType;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.dashboard.query.application.port.FestivalDashboardMetricProvider;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapZone;
import com.example.chookjibupadmin.operator.command.application.FieldStaffAccountService;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 승인 부스·혼잡 이력·로드맵 구역으로 대시보드 실지표를 계산한다.
 * 실시간 현재 방문자 수는 별도 공급이 없어 visitorAvailable=false로 둔다.
 */
@Component
@RequiredArgsConstructor
public class BoothCongestionFestivalDashboardMetricProvider
        implements FestivalDashboardMetricProvider {

    private static final String SCHEMA_WGS84 = "2.0";

    private final BoothInfoService boothInfoService;
    private final BoothCongestionService boothCongestionService;
    private final FestivalService festivalService;
    private final FestivalRoadmapService festivalRoadmapService;
    private final RoadmapNodeService roadmapNodeService;
    private final AdminAccountService adminAccountService;
    private final FieldStaffAccountService fieldStaffAccountService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public Optional<Snapshot> findCurrent(Long festivalId) {
        Festival festival = festivalService.getById(festivalId);
        List<BoothInfo> booths = boothInfoService.findAllByFestivalId(festivalId);
        boolean boothAvailable = !booths.isEmpty();

        List<Long> boothIds = booths.stream().map(BoothInfo::getId).toList();
        List<BoothCongestion> latest =
                boothCongestionService.findLatestByBoothIds(boothIds);
        Map<Long, BoothCongestion> latestByBooth = latest.stream()
                .collect(Collectors.toMap(
                        BoothCongestion::getBoothId,
                        Function.identity(),
                        (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b
                ));

        boolean congestionAvailable = !latestByBooth.isEmpty();
        boolean visitorAvailable = false;
        Long currentVisitorCount = null;

        Long activeQueueCount = null;
        Long averageWaitMinutes = null;
        LocalDateTime updatedAt = null;
        if (congestionAvailable) {
            long active = latestByBooth.values().stream()
                    .filter(this::isActiveQueue)
                    .count();
            double avg = latestByBooth.values().stream()
                    .mapToInt(c -> c.getWaitMinutes() == null ? 0 : c.getWaitMinutes())
                    .average()
                    .orElse(0d);
            activeQueueCount = active;
            averageWaitMinutes = Math.round(avg);
            updatedAt = latestByBooth.values().stream()
                    .map(BoothCongestion::getCreatedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
        }

        boolean summaryAvailable = congestionAvailable;
        String operatingStatus = resolveOperatingStatus(festival);

        Map<Long, RoadmapNode> nodesById = loadNodes(booths);
        Map<Long, String> adminNames = loadAdminNames(latestByBooth.values());
        Map<Long, String> staffNames = loadStaffNames(latestByBooth.values());

        List<BoothMetric> boothMetrics = new ArrayList<>();
        for (BoothInfo booth : booths) {
            BoothCongestion congestion = latestByBooth.get(booth.getId());
            RoadmapNode node = nodesById.get(booth.getRoadmapNodeId());
            Wgs84Point point = extractWgs84Point(node);
            boothMetrics.add(new BoothMetric(
                    booth.getId(),
                    booth.getBoothName(),
                    node == null ? null : node.getPublicId(),
                    point == null ? null : point.lat(),
                    point == null ? null : point.lng(),
                    congestion == null ? null : congestion.getCongestionLevel().name(),
                    congestion == null ? null : congestion.getWaitMinutes(),
                    congestion == null ? null : congestion.getCreatedAt(),
                    congestion == null || congestion.getModifierType() == null
                            ? null
                            : congestion.getModifierType().name(),
                    congestion == null ? null : congestion.getModifierAdminId(),
                    congestion == null ? null : congestion.getModifierStaffId(),
                    resolveModifierName(congestion, adminNames, staffNames)
            ));
        }

        List<ZoneMetric> zones = festivalRoadmapService.findByFestivalId(festivalId)
                .map(roadmap -> roadmap.getZones().stream()
                        .map(this::toZoneMetric)
                        .toList())
                .orElse(List.of());

        if (!boothAvailable && !congestionAvailable && !visitorAvailable) {
            return Optional.empty();
        }

        return Optional.of(new Snapshot(
                visitorAvailable,
                boothAvailable,
                congestionAvailable,
                summaryAvailable,
                operatingStatus,
                currentVisitorCount,
                activeQueueCount,
                averageWaitMinutes,
                updatedAt,
                boothMetrics,
                zones
        ));
    }

    private Map<Long, RoadmapNode> loadNodes(List<BoothInfo> booths) {
        Set<Long> nodeIds = booths.stream()
                .map(BoothInfo::getRoadmapNodeId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        if (nodeIds.isEmpty()) {
            return Map.of();
        }
        return roadmapNodeService.findAllById(nodeIds).stream()
                .collect(Collectors.toMap(RoadmapNode::getId, Function.identity()));
    }

    private Map<Long, String> loadAdminNames(Iterable<BoothCongestion> congestions) {
        Set<Long> adminIds = new HashSet<>();
        for (BoothCongestion congestion : congestions) {
            if (congestion.getModifierType() == BoothCongestionModifierType.ADMIN
                    && congestion.getModifierAdminId() != null) {
                adminIds.add(congestion.getModifierAdminId());
            }
        }
        if (adminIds.isEmpty()) {
            return Map.of();
        }
        return adminAccountService.findAllById(adminIds).stream()
                .collect(Collectors.toMap(
                        AdminAccount::getId,
                        AdminAccount::getNameValue,
                        (a, b) -> a
                ));
    }

    private Map<Long, String> loadStaffNames(Iterable<BoothCongestion> congestions) {
        Set<Long> staffIds = new HashSet<>();
        for (BoothCongestion congestion : congestions) {
            if (congestion.getModifierType() == BoothCongestionModifierType.STAFF
                    && congestion.getModifierStaffId() != null) {
                staffIds.add(congestion.getModifierStaffId());
            }
        }
        if (staffIds.isEmpty()) {
            return Map.of();
        }
        return fieldStaffAccountService.findAllById(staffIds).stream()
                .collect(Collectors.toMap(
                        FieldStaffAccount::getId,
                        FieldStaffAccount::getNameValue,
                        (a, b) -> a
                ));
    }

    private String resolveModifierName(
            BoothCongestion congestion,
            Map<Long, String> adminNames,
            Map<Long, String> staffNames
    ) {
        if (congestion == null || congestion.getModifierType() == null) {
            return null;
        }
        return switch (congestion.getModifierType()) {
            case ADMIN -> adminNames.get(congestion.getModifierAdminId());
            case STAFF -> staffNames.get(congestion.getModifierStaffId());
        };
    }

    private ZoneMetric toZoneMetric(RoadmapZone zone) {
        return new ZoneMetric(
                zone.zoneId(),
                zone.name(),
                zone.sortOrder(),
                zone.boothNodeIds()
        );
    }

    private Wgs84Point extractWgs84Point(RoadmapNode node) {
        if (node == null
                || !SCHEMA_WGS84.equals(node.getGeometrySchemaVersion())
                || node.getGeometryType() != GeometryType.POINT
                || node.getGeometryData() == null) {
            return null;
        }
        try {
            JsonNode geometry = objectMapper.readTree(node.getGeometryData());
            JsonNode latNode = geometry.get("lat");
            JsonNode lngNode = geometry.get("lng");
            if (latNode == null || lngNode == null || !latNode.isNumber() || !lngNode.isNumber()) {
                return null;
            }
            return new Wgs84Point(
                    latNode.decimalValue(),
                    lngNode.decimalValue()
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isActiveQueue(BoothCongestion congestion) {
        if (congestion.getCongestionLevel() == BoothCongestionLevel.LOW) {
            return false;
        }
        Integer wait = congestion.getWaitMinutes();
        return wait != null && wait > 0;
    }

    private String resolveOperatingStatus(Festival festival) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = festival.getPeriod().getStartDate();
        LocalDate end = festival.getPeriod().getEndDate();
        if (today.isBefore(start)) {
            return "PREPARING";
        }
        if (today.isAfter(end)) {
            return "ENDED";
        }
        return "ONGOING";
    }

    private record Wgs84Point(BigDecimal lat, BigDecimal lng) {
    }
}
