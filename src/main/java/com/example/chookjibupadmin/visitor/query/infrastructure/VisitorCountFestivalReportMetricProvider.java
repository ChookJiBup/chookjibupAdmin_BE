package com.example.chookjibupadmin.visitor.query.infrastructure;

import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.report.query.application.port.FestivalReportMetricProvider;
import com.example.chookjibupadmin.visitor.command.application.FestivalVisitorCountService;
import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
import com.example.chookjibupadmin.visitor.support.FestivalVisitorInputSupport;
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

    @Override
    public Optional<Snapshot> findSummary(Long festivalId) {
        Festival festival = festivalService.getById(festivalId);
        var snapshot = FestivalVisitorInputSupport.resolve(
                festival,
                visitorCountService.findDailyByFestivalIdOrderByVisitDateAsc(festivalId),
                visitorCountService.findTotalByFestivalId(festivalId)
                        .map(FestivalTotalVisitorCount::getVisitorCountValue)
        );
        if (!FestivalVisitorInputSupport.isReportReady(snapshot)
                || snapshot.effectiveVisitorCount() == null) {
            return Optional.empty();
        }
        return Optional.of(new Snapshot(
                snapshot.effectiveVisitorCount(),
                0L,
                0L,
                null
        ));
    }
}
