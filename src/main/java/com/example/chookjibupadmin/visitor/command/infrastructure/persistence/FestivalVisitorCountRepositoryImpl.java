package com.example.chookjibupadmin.visitor.command.infrastructure.persistence;

import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
import com.example.chookjibupadmin.visitor.command.domain.FestivalVisitorCountRepository;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FestivalVisitorCountRepositoryImpl
        implements FestivalVisitorCountRepository {

    private final FestivalDailyVisitorCountJpaRepository dailyJpaRepository;
    private final FestivalTotalVisitorCountJpaRepository totalJpaRepository;

    @Override
    public FestivalDailyVisitorCount saveDaily(
            FestivalDailyVisitorCount dailyVisitorCount
    ) {
        return dailyJpaRepository.save(dailyVisitorCount);
    }

    @Override
    public Optional<FestivalDailyVisitorCount> findDailyByFestivalIdAndVisitDateForUpdate(
            Long festivalId,
            LocalDate visitDate
    ) {
        return dailyJpaRepository.findByFestivalIdAndVisitDateForUpdate(
                festivalId,
                visitDate
        );
    }

    @Override
    public FestivalTotalVisitorCount saveTotal(
            FestivalTotalVisitorCount totalVisitorCount
    ) {
        return totalJpaRepository.save(totalVisitorCount);
    }

    @Override
    public Optional<FestivalTotalVisitorCount> findTotalByFestivalIdForUpdate(
            Long festivalId
    ) {
        return totalJpaRepository.findByFestivalIdForUpdate(festivalId);
    }
}
