package com.example.chookjibupadmin.festival.command.infrastructure.persistence;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FestivalJpaRepository extends JpaRepository<Festival, Long> {

    Optional<Festival> findByPublicId(UUID publicId);

    boolean existsBySeriesIdAndYear(Long seriesId, int year);
}
