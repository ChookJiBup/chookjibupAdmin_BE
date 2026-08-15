package com.example.chookjibupadmin.visitor.command.infrastructure.persistence;

import com.example.chookjibupadmin.visitor.command.domain.FestivalTotalVisitorCount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface FestivalTotalVisitorCountJpaRepository
        extends JpaRepository<FestivalTotalVisitorCount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select v
        from FestivalTotalVisitorCount v
        where v.festivalId = :festivalId
    """)
    Optional<FestivalTotalVisitorCount> findByFestivalIdForUpdate(
            @Param("festivalId") Long festivalId
    );
}
