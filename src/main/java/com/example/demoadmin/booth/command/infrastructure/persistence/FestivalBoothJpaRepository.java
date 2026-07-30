package com.example.demoadmin.booth.command.infrastructure.persistence;

import com.example.demoadmin.booth.command.domain.FestivalBooth;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

interface FestivalBoothJpaRepository extends JpaRepository<FestivalBooth, Long> {

    Optional<FestivalBooth> findByFestivalIdAndPublicId(
            Long festivalId,
            UUID publicId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select booth
            from FestivalBooth booth
            where booth.festivalId = :festivalId
              and booth.publicId = :publicId
            """)
    Optional<FestivalBooth> findByFestivalIdAndPublicIdForUpdate(
            @Param("festivalId") Long festivalId,
            @Param("publicId") UUID publicId
    );
}
