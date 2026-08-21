package com.example.chookjibupadmin.visitor.command.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 축제 방문 인원 수 저장소 계약이다.
 */
public interface FestivalVisitorCountRepository {

    FestivalDailyVisitorCount saveDaily(
            FestivalDailyVisitorCount dailyVisitorCount
    );

    Optional<FestivalDailyVisitorCount> findDailyByFestivalIdAndVisitDateForUpdate(
            Long festivalId,
            LocalDate visitDate
    );

    List<FestivalDailyVisitorCount> findDailyByFestivalIdOrderByVisitDateAsc(
            Long festivalId
    );

    FestivalTotalVisitorCount saveTotal(
            FestivalTotalVisitorCount totalVisitorCount
    );

    Optional<FestivalTotalVisitorCount> findTotalByFestivalIdForUpdate(
            Long festivalId
    );

    Optional<FestivalTotalVisitorCount> findTotalByFestivalId(Long festivalId);
}
