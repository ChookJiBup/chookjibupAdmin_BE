package com.example.chookjibupadmin.visitor.command.infrastructure.persistence;

import com.example.chookjibupadmin.visitor.command.domain.FestivalDailyVisitorCount;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface FestivalDailyVisitorCountJpaRepository
        extends JpaRepository<FestivalDailyVisitorCount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select v
        from FestivalDailyVisitorCount v
        where v.festivalId = :festivalId
          and v.visitDate = :visitDate
    """)
    Optional<FestivalDailyVisitorCount> findByFestivalIdAndVisitDateForUpdate(
            @Param("festivalId") Long festivalId,
            @Param("visitDate") LocalDate visitDate
    );

    List<FestivalDailyVisitorCount> findByFestivalIdOrderByVisitDateAsc(
            Long festivalId
    );
}
