package com.example.demoadmin.map.command.infrastructure.persistence;

import com.example.demoadmin.map.command.domain.MapAnalysisJob;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MapAnalysisJobJpaRepository extends JpaRepository<MapAnalysisJob, Long> {

    Optional<MapAnalysisJob> findByPublicId(UUID publicId);
}
