package com.example.chookjibupadmin.festival.command.infrastructure.persistence;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FestivalRepositoryImpl implements FestivalRepository {

    private final FestivalJpaRepository jpaRepository;

    @Override
    public int synchronizeProgressStatuses() {
        return jpaRepository.synchronizeProgressStatuses();
    }

    @Override
    public Festival save(Festival festival) {
        return jpaRepository.save(festival);
    }

    @Override
    public void delete(Festival festival) {
        jpaRepository.delete(festival);
    }

    @Override
    public Optional<Festival> findById(Long festivalId) {
        return jpaRepository.findById(festivalId);
    }

    @Override
    public Optional<Festival> findByIdForUpdate(Long festivalId) {
        return jpaRepository.findByIdForUpdate(festivalId);
    }

    @Override
    public Optional<Festival> findByPublicId(UUID publicId) {
        return jpaRepository.findByPublicId(publicId);
    }

    @Override
    public Optional<Festival> findByPublicIdForUpdate(UUID publicId) {
        return jpaRepository.findByPublicIdForUpdate(publicId);
    }

    @Override
    public boolean existsBySeriesIdAndYear(Long seriesId, int year) {
        return jpaRepository.existsBySeriesIdAndYear(seriesId, year);
    }

    @Override
    public Optional<Festival> findBySeriesIdAndYear(Long seriesId, int year) {
        return jpaRepository.findBySeriesIdAndYear(seriesId, year);
    }

    @Override
    public List<Festival> findAllWithLegacyRepresentativeImageUrl() {
        return jpaRepository.findAllWithLegacyRepresentativeImageUrl();
    }
}
