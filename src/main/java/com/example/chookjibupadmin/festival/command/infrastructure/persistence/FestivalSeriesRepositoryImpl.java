package com.example.chookjibupadmin.festival.command.infrastructure.persistence;

import com.example.chookjibupadmin.festival.command.domain.FestivalSeries;
import com.example.chookjibupadmin.festival.command.domain.FestivalSeriesRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FestivalSeriesRepositoryImpl implements FestivalSeriesRepository {

    private final FestivalSeriesJpaRepository jpaRepository;

    @Override
    public FestivalSeries save(FestivalSeries festivalSeries) {
        return jpaRepository.save(festivalSeries);
    }

    @Override
    public Optional<FestivalSeries> findById(Long seriesId) {
        return jpaRepository.findById(seriesId);
    }

    @Override
    public Optional<FestivalSeries> findByPublicId(UUID publicId) {
        return jpaRepository.findByPublicId(publicId);
    }

    @Override
    public Optional<FestivalSeries> findByNormalizedName(String normalizedName) {
        return jpaRepository.findByNormalizedName(normalizedName);
    }
}
