package com.example.chookjibupadmin.visitor.query.infrastructure;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import com.example.chookjibupadmin.report.query.application.port.FestivalReportMetricProvider;
import com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService;
import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorInputSupport;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorInputStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 방문 인원 투트랙({@link FestivalVisitorInputSupport})으로 결과 보고서 요약 지표를 제공한다.
 */
@Repository
@RequiredArgsConstructor
public class VisitorCountFestivalReportMetricProvider
        implements FestivalReportMetricProvider {

    private final FestivalService festivalService;
    private final FestivalVisitorCountService visitorCountService;
    private final Clock clock;

    @Override
    public Optional<Snapshot> findSummary(Long festivalId) {
        Festival festival = festivalService.getById(festivalId);
        List<FestivalDailyVisitorCount> dailyCounts = visitorCountService
                .findDailyByFestivalIdOrderByVisitDateAsc(festivalId);
        var snapshot = FestivalVisitorInputSupport.resolve(
                festival,
                dailyCounts,
                visitorCountService.findTotalByFestivalId(festivalId)
                        .map(FestivalTotalVisitorCount::getVisitorCountValue)
        );
        Long visitorCount = snapshot.effectiveVisitorCount();
        if (visitorCount == null
                && !dailyCounts.isEmpty()
                && (snapshot.status() == FestivalVisitorInputStatus.PARTIAL
                || snapshot.status() == FestivalVisitorInputStatus.UNSET)
                && FestivalProgressStatus.from(
                        LocalDate.now(clock),
                        festival.getStartDate(),
                        festival.getEndDate()
                ) == FestivalProgressStatus.ONGOING) {
            LocalDate today = LocalDate.now(clock);
            visitorCount = dailyCounts.stream()
                    .filter(count -> !count.getVisitDate().isBefore(
                            festival.getStartDate()
                    ))
                    .filter(count -> !count.getVisitDate().isAfter(
                            festival.getEndDate()
                    ))
                    .filter(count -> !count.getVisitDate().isAfter(today))
                    .mapToLong(FestivalDailyVisitorCount::getVisitorCountValue)
                    .sum();
        }
        if (visitorCount == null) {
            return Optional.empty();
        }
        return Optional.of(new Snapshot(
                visitorCount,
                0L,
                0L,
                null
        ));
    }
}
