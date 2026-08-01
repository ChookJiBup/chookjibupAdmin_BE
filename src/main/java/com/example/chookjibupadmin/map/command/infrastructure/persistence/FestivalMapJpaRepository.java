package com.example.chookjibupadmin.map.command.infrastructure.persistence;

import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FestivalMapJpaRepository extends JpaRepository<FestivalMap, Long> {

    Optional<FestivalMap> findByPublicId(UUID publicId);
}
