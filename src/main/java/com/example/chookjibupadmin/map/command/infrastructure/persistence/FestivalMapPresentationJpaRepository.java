package com.example.chookjibupadmin.map.command.infrastructure.persistence;

import com.example.chookjibupadmin.map.command.domain.FestivalMapPresentation;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FestivalMapPresentationJpaRepository
        extends JpaRepository<FestivalMapPresentation, Long> {

    Optional<FestivalMapPresentation> findByMapId(Long mapId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from FestivalMapPresentation p where p.mapId = :mapId")
    Optional<FestivalMapPresentation> findByMapIdForUpdate(@Param("mapId") Long mapId);

    void deleteByMapIdIn(Collection<Long> mapIds);
}
