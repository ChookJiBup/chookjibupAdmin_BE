package com.example.demoadmin.map.command.infrastructure.persistence;

import com.example.demoadmin.map.command.domain.FestivalMap;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FestivalMapJpaRepository extends JpaRepository<FestivalMap, Long> {

    Optional<FestivalMap> findByFestivalIdAndPublicId(
            Long festivalId,
            UUID publicId
    );
}
