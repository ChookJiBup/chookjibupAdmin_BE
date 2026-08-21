package com.example.chookjibupadmin.visitor.command.application;

import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.FestivalVisitorCountRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 방문 인원 수 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalVisitorCountService {

    private final FestivalVisitorCountRepository repository;

    @Transactional
    public FestivalDailyVisitorCount saveDaily(
            FestivalDailyVisitorCount dailyVisitorCount
    ) {
        return repository.saveDaily(dailyVisitorCount);
    }

    public Optional<FestivalDailyVisitorCount> findDailyByFestivalIdAndVisitDateForUpdate(
            Long festivalId,
            LocalDate visitDate
    ) {
        return repository.findDailyByFestivalIdAndVisitDateForUpdate(
                festivalId,
                visitDate
        );
    }

    public List<FestivalDailyVisitorCount> findDailyByFestivalIdOrderByVisitDateAsc(
            Long festivalId
    ) {
        return repository.findDailyByFestivalIdOrderByVisitDateAsc(festivalId);
    }

    @Transactional
    public FestivalTotalVisitorCount saveTotal(
            FestivalTotalVisitorCount totalVisitorCount
    ) {
        return repository.saveTotal(totalVisitorCount);
    }

    public Optional<FestivalTotalVisitorCount> findTotalByFestivalIdForUpdate(
            Long festivalId
    ) {
        return repository.findTotalByFestivalIdForUpdate(festivalId);
    }

    public Optional<FestivalTotalVisitorCount> findTotalByFestivalId(
            Long festivalId
    ) {
        return repository.findTotalByFestivalId(festivalId);
    }
}
