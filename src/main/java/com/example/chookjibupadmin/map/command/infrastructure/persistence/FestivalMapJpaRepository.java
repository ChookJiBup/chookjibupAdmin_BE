package com.example.chookjibupadmin.map.command.infrastructure.persistence;

import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface FestivalMapJpaRepository extends JpaRepository<FestivalMap, Long> {

    Optional<FestivalMap> findByPublicId(UUID publicId);

    boolean existsByLocationId(Long locationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select fm from FestivalMap fm where fm.publicId = :publicId")
    Optional<FestivalMap> findByPublicIdForUpdate(
            @Param("publicId") UUID publicId
    );

    @Query("""
            select fm from FestivalMap fm
            where fm.festivalId = :festivalId
              and fm.current = true
              and fm.storageStatus <> 'DELETED'
            """)
    Optional<FestivalMap> findCurrentByFestivalId(@Param("festivalId") Long festivalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select fm from FestivalMap fm where fm.festivalId = :festivalId order by fm.id")
    List<FestivalMap> findAllByFestivalIdForUpdate(
            @Param("festivalId") Long festivalId
    );
}
