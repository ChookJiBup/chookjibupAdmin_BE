package com.example.chookjibupadmin.dashboard.query.infrastructure;

import com.example.chookjibupadmin.booth.command.application.BoothCongestionService;
import com.example.chookjibupadmin.booth.command.application.BoothInfoService;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.dashboard.query.application.port.FestivalDashboardMetricProvider;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 승인 부스·혼잡 이력으로 대시보드 실지표를 계산한다.
 * 실시간 현재 방문자 수는 별도 공급이 없어 visitorAvailable=false로 둔다.
 */
@Component
@RequiredArgsConstructor
public class BoothCongestionFestivalDashboardMetricProvider
        implements FestivalDashboardMetricProvider {

    private final BoothInfoService boothInfoService;
    private final BoothCongestionService boothCongestionService;
    private final FestivalService festivalService;
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

        List<BoothMetric> boothMetrics = new ArrayList<>();
        for (BoothInfo booth : booths) {
            BoothCongestion congestion = latestByBooth.get(booth.getId());
            boothMetrics.add(new BoothMetric(
                    booth.getId(),
                    booth.getBoothName(),
                    congestion == null ? null : congestion.getCongestionLevel().name(),
                    congestion == null ? null : congestion.getWaitMinutes(),
                    congestion == null ? null : congestion.getCreatedAt()
            ));
        }

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
                boothMetrics
        ));
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
}
