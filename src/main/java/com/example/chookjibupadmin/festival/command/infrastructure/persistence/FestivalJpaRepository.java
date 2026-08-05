package com.example.chookjibupadmin.festival.command.infrastructure.persistence;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface FestivalJpaRepository extends JpaRepository<Festival, Long> {

    Optional<Festival> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Festival f where f.publicId = :publicId")
    Optional<Festival> findByPublicIdForUpdate(@Param("publicId") UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Festival f where f.id = :festivalId")
    Optional<Festival> findByIdForUpdate(@Param("festivalId") Long festivalId);

    boolean existsBySeriesIdAndYear(Long seriesId, int year);
}
