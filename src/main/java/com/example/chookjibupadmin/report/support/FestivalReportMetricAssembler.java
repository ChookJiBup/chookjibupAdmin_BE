package com.example.chookjibupadmin.report.support;

import com.example.chookjibupadmin.booth.command.application.BoothCongestionService;
import com.example.chookjibupadmin.booth.command.application.BoothInfoService;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService;
import com.example.chookjibupadmin.map.roadmap.application.RoadmapNodeService;
import com.example.chookjibupadmin.map.roadmap.domain.FestivalRoadmap;
import com.example.chookjibupadmin.map.roadmap.domain.RoadmapNode;
import com.example.chookjibupadmin.report.support.dto.FestivalBoothCongestionShareItem;
import com.example.chookjibupadmin.report.support.dto.FestivalDailyVisitorTrendPoint;
import com.example.chookjibupadmin.report.support.dto.FestivalEconomicEffectMetric;
import com.example.chookjibupadmin.report.support.dto.FestivalOperationEfficiencyMetric;
import com.example.chookjibupadmin.report.support.dto.FestivalReportMetrics;
import com.example.chookjibupadmin.report.support.dto.FestivalTotalVisitorMetric;
import com.example.chookjibupadmin.report.support.dto.FestivalVisitPatternMetric;
import com.example.chookjibupadmin.report.support.dto.FestivalVisitorChangeDirection;
import com.example.chookjibupadmin.report.support.dto.FestivalZoneWaitRankingItem;
import com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorDaySupport;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorEffectiveSource;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorInputSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 축제 방문 인원과 시리즈 전년 축제로 성과 리포트 집계 지표를 조립한다.
 */
@Component
@RequiredArgsConstructor
public class FestivalReportMetricAssembler {

    private final FestivalService festivalService;
    private final FestivalVisitorCountService visitorCountService;
    private final BoothCongestionService boothCongestionService;
    private final BoothInfoService boothInfoService;
    private final FestivalRoadmapService roadmapService;
    private final RoadmapNodeService roadmapNodeService;
    private final Clock clock;

    public FestivalReportMetrics assemble(Festival festival) {
        List<FestivalDailyVisitorCount> currentDaily = visitorCountService
                .findDailyByFestivalIdOrderByVisitDateAsc(festival.getId());
        Optional<Integer> currentTotal = visitorCountService
                .findTotalByFestivalId(festival.getId())
                .map(FestivalTotalVisitorCount::getVisitorCountValue);
        var snapshot = FestivalVisitorInputSupport.resolve(
                festival,
                currentDaily,
                currentTotal
        );

        Optional<Festival> previous = findPreviousFestival(festival);
        long previousTotal = previous
                .map(this::resolveEffectiveTotal)
                .orElse(0L);

        List<FestivalDailyVisitorTrendPoint> dailyTrend =
                buildDailyTrend(festival, currentDaily, previous, snapshot);

        // 진행 중에는 아직 남은 일차가 있어 READY가 아니더라도,
        // 마감된 일차까지의 누적 인원은 운영리포트에 바로 보여 준다.
        boolean ongoing = progressStatus(festival) == FestivalProgressStatus.ONGOING;
        long currentEffective = snapshot.effectiveVisitorCount() == null
                ? ongoing ? sumDailyThroughToday(festival, currentDaily) : 0L
                : snapshot.effectiveVisitorCount();
        boolean completedVisitorInput = FestivalVisitorInputSupport.isReportReady(snapshot);
        List<BoothCongestion> congestionHistory = boothCongestionService
                .findAllByFestivalId(festival.getId());
        List<BoothInfo> booths = congestionHistory.isEmpty()
                ? List.of()
                : boothInfoService.findAllByFestivalId(festival.getId());

        return new FestivalReportMetrics(
                festival.getPublicId(),
                festival.getNameValue(),
                festival.getYear() == null ? 0 : festival.getYear(),
                FestivalVisitorDaySupport.totalDayCount(festival),
                completedVisitorInput,
                buildTotalVisitors(
                        currentEffective,
                        previous.isPresent() && completedVisitorInput,
                        previousTotal
                ),
                dailyTrend,
                FestivalEconomicEffectMetric.unavailable(),
                buildOperationEfficiency(congestionHistory),
                buildZoneWaitRanking(festival.getId(), congestionHistory, booths),
                buildBoothCongestionShare(congestionHistory),
                buildCongestionPeakHours(congestionHistory)
        );
    }

    private FestivalOperationEfficiencyMetric buildOperationEfficiency(
            List<BoothCongestion> history
    ) {
        if (history.isEmpty()) {
            return FestivalOperationEfficiencyMetric.unavailable();
        }
        long averageWaitMinutes = Math.round(history.stream()
                .mapToInt(BoothCongestion::getWaitMinutes)
                .average()
                .orElse(0));
        int boothCount = (int) history.stream()
                .map(BoothCongestion::getBoothId)
                .distinct()
                .count();
        return new FestivalOperationEfficiencyMetric(true, averageWaitMinutes, boothCount);
    }

    private List<FestivalZoneWaitRankingItem> buildZoneWaitRanking(
            Long festivalId,
            List<BoothCongestion> history,
            List<BoothInfo> booths
    ) {
        if (history.isEmpty()) {
            return List.of();
        }
        Map<Long, String> groupNameByBoothId = resolveGroupNames(festivalId, booths);
        Map<String, List<BoothCongestion>> byGroup = history.stream()
                .filter(item -> groupNameByBoothId.containsKey(item.getBoothId()))
                .collect(Collectors.groupingBy(
                        item -> groupNameByBoothId.get(item.getBoothId())
                ));
        List<Map.Entry<String, Long>> ranked = byGroup.entrySet().stream()
                .map(entry -> Map.entry(
                        entry.getKey(),
                        Math.round(entry.getValue().stream()
                                .mapToInt(BoothCongestion::getWaitMinutes)
                                .average()
                                .orElse(0))
                ))
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .toList();
        List<FestivalZoneWaitRankingItem> result = new ArrayList<>();
        for (int index = 0; index < ranked.size(); index++) {
            var item = ranked.get(index);
            result.add(new FestivalZoneWaitRankingItem(
                    index + 1,
                    item.getKey(),
                    item.getValue()
            ));
        }
        return result;
    }

    private Map<Long, String> resolveGroupNames(Long festivalId, List<BoothInfo> booths) {
        Map<Long, String> names = booths.stream().collect(Collectors.toMap(
                BoothInfo::getId,
                BoothInfo::getBoothName
        ));
        Optional<FestivalRoadmap> roadmap = roadmapService.findByFestivalId(festivalId);
        if (roadmap.isEmpty() || roadmap.get().getZones().isEmpty()) {
            return names;
        }

        List<Long> nodeIds = booths.stream()
                .map(BoothInfo::getRoadmapNodeId)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, UUID> publicIdByNodeId = roadmapNodeService.findAllById(nodeIds).stream()
                .collect(Collectors.toMap(RoadmapNode::getId, RoadmapNode::getPublicId));
        Map<UUID, Long> boothIdByNodePublicId = booths.stream()
                .filter(booth -> publicIdByNodeId.containsKey(booth.getRoadmapNodeId()))
                .collect(Collectors.toMap(
                        booth -> publicIdByNodeId.get(booth.getRoadmapNodeId()),
                        BoothInfo::getId
                ));
        for (var zone : roadmap.get().getZones()) {
            for (UUID nodePublicId : zone.boothNodeIds()) {
                Long boothId = boothIdByNodePublicId.get(nodePublicId);
                if (boothId != null) {
                    names.put(boothId, zone.name());
                }
            }
        }
        return names;
    }

    private List<FestivalBoothCongestionShareItem> buildBoothCongestionShare(
            List<BoothCongestion> history
    ) {
        if (history.isEmpty()) {
            return List.of();
        }
        Map<Long, BoothCongestion> latestByBooth = new LinkedHashMap<>();
        history.stream()
                .sorted(Comparator.comparing(BoothCongestion::getCreatedAt)
                        .thenComparing(BoothCongestion::getId))
                .forEach(item -> latestByBooth.put(item.getBoothId(), item));
        Map<BoothCongestionLevel, Long> counts = latestByBooth.values().stream()
                .collect(Collectors.groupingBy(
                        BoothCongestion::getCongestionLevel,
                        Collectors.counting()
                ));
        BigDecimal total = BigDecimal.valueOf(latestByBooth.size());
        return List.of(
                BoothCongestionLevel.HIGH,
                BoothCongestionLevel.MEDIUM,
                BoothCongestionLevel.LOW
        ).stream()
                .map(level -> new FestivalBoothCongestionShareItem(
                        level.name(),
                        BigDecimal.valueOf(counts.getOrDefault(level, 0L))
                                .multiply(BigDecimal.valueOf(100))
                                .divide(total, 2, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    private FestivalVisitPatternMetric buildCongestionPeakHours(
            List<BoothCongestion> history
    ) {
        if (history.isEmpty()) {
            return FestivalVisitPatternMetric.unavailable();
        }
        Map<Integer, List<BoothCongestion>> byHour = history.stream()
                .collect(Collectors.groupingBy(item -> item.getCreatedAt().getHour()));
        List<String> peakHours = byHour.entrySet().stream()
                .map(entry -> Map.entry(
                        entry.getKey(),
                        entry.getValue().stream()
                                .mapToInt(BoothCongestion::getWaitMinutes)
                                .average()
                                .orElse(0)
                ))
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(3)
                .map(entry -> String.format(
                        "%02d:00~%02d:00",
                        entry.getKey(),
                        (entry.getKey() + 1) % 24
                ))
                .toList();
        return new FestivalVisitPatternMetric(!peakHours.isEmpty(), peakHours);
    }

    private List<FestivalDailyVisitorTrendPoint> buildDailyTrend(
            Festival festival,
            List<FestivalDailyVisitorCount> currentDaily,
            Optional<Festival> previous,
            FestivalVisitorInputSupport.FestivalVisitorInputSnapshot snapshot
    ) {
        if (snapshot.inputMode() == FestivalVisitorCountInputMode.TOTAL
                || snapshot.source() == FestivalVisitorEffectiveSource.TOTAL) {
            return List.of();
        }

        Map<LocalDate, Integer> currentByDate = toCountMap(currentDaily);
        Map<Integer, Integer> previousByDayIndex = previous
                .map(this::dailyCountsByDayIndex)
                .orElse(Map.of());

        List<FestivalDailyVisitorTrendPoint> dailyTrend = new ArrayList<>();
        int dayIndex = 1;
        LocalDate cursor = festival.getStartDate();
        LocalDate today = LocalDate.now(clock);
        LocalDate end = festival.getEndDate();
        if (progressStatus(festival) == FestivalProgressStatus.ONGOING
                && today.isBefore(end)) {
            end = today;
        }
        while (!cursor.isAfter(end)) {
            Integer currentCount = currentByDate.get(cursor);
            Integer previousCount = previousByDayIndex.get(dayIndex);
            dailyTrend.add(new FestivalDailyVisitorTrendPoint(
                    dayIndex,
                    cursor,
                    currentCount == null ? null : currentCount.longValue(),
                    previousCount == null ? null : previousCount.longValue()
            ));
            dayIndex++;
            cursor = cursor.plusDays(1);
        }
        return dailyTrend;
    }

    private long resolveEffectiveTotal(Festival festival) {
        var snapshot = FestivalVisitorInputSupport.resolve(
                festival,
                visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(
                        festival.getId()
                ),
                visitorCountService.findTotalByFestivalId(festival.getId())
                        .map(FestivalTotalVisitorCount::getVisitorCountValue)
        );
        return snapshot.effectiveVisitorCount() == null
                ? 0L
                : snapshot.effectiveVisitorCount();
    }

    private long sumDailyThroughToday(
            Festival festival,
            List<FestivalDailyVisitorCount> dailyCounts
    ) {
        LocalDate today = LocalDate.now(clock);
        return dailyCounts.stream()
                .filter(count -> !count.getVisitDate().isBefore(festival.getStartDate()))
                .filter(count -> !count.getVisitDate().isAfter(festival.getEndDate()))
                .filter(count -> !count.getVisitDate().isAfter(today))
                .mapToLong(FestivalDailyVisitorCount::getVisitorCountValue)
                .sum();
    }

    private FestivalProgressStatus progressStatus(Festival festival) {
        return festival.progressStatus(LocalDate.now(clock));
    }

    private FestivalTotalVisitorMetric buildTotalVisitors(
            long current,
            boolean hasPrevious,
            long previousTotal
    ) {
        if (!hasPrevious) {
            return new FestivalTotalVisitorMetric(
                    current,
                    null,
                    null,
                    null,
                    FestivalVisitorChangeDirection.NONE
            );
        }

        long delta = current - previousTotal;
        BigDecimal rate = null;
        FestivalVisitorChangeDirection direction;
        if (previousTotal == 0L) {
            direction = current == 0L
                    ? FestivalVisitorChangeDirection.FLAT
                    : FestivalVisitorChangeDirection.NONE;
        } else {
            rate = BigDecimal.valueOf(delta)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            BigDecimal.valueOf(previousTotal),
                            1,
                            RoundingMode.HALF_UP
                    );
            if (delta > 0) {
                direction = FestivalVisitorChangeDirection.UP;
            } else if (delta < 0) {
                direction = FestivalVisitorChangeDirection.DOWN;
            } else {
                direction = FestivalVisitorChangeDirection.FLAT;
            }
        }

        return new FestivalTotalVisitorMetric(
                current,
                previousTotal,
                delta,
                rate,
                direction
        );
    }

    private Optional<Festival> findPreviousFestival(Festival festival) {
        if (festival.getSeriesId() == null || festival.getYear() == null) {
            return Optional.empty();
        }
        return festivalService.findBySeriesIdAndYear(
                festival.getSeriesId(),
                festival.getYear() - 1
        );
    }

    private Map<Integer, Integer> dailyCountsByDayIndex(Festival festival) {
        List<FestivalDailyVisitorCount> daily = visitorCountService
                .findDailyByFestivalIdOrderByVisitDateAsc(festival.getId());
        Map<LocalDate, Integer> byDate = toCountMap(daily);
        Map<Integer, Integer> byIndex = new java.util.HashMap<>();
        int dayIndex = 1;
        LocalDate cursor = festival.getStartDate();
        LocalDate end = festival.getEndDate();
        while (!cursor.isAfter(end)) {
            Integer count = byDate.get(cursor);
            if (count != null) {
                byIndex.put(dayIndex, count);
            }
            dayIndex++;
            cursor = cursor.plusDays(1);
        }
        return byIndex;
    }

    private Map<LocalDate, Integer> toCountMap(
            List<FestivalDailyVisitorCount> dailyCounts
    ) {
        return dailyCounts.stream().collect(Collectors.toMap(
                FestivalDailyVisitorCount::getVisitDate,
                FestivalDailyVisitorCount::getVisitorCountValue,
                (left, right) -> right
        ));
    }
}
