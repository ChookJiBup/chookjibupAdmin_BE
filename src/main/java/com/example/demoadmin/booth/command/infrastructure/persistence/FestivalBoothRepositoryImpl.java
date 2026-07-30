package com.example.demoadmin.booth.command.infrastructure.persistence;

import com.example.demoadmin.booth.command.domain.FestivalBooth;
import com.example.demoadmin.booth.command.domain.FestivalBoothRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FestivalBoothRepositoryImpl implements FestivalBoothRepository {

    private final FestivalBoothJpaRepository jpaRepository;

    @Override
    public FestivalBooth save(FestivalBooth booth) {
        return jpaRepository.save(booth);
    }

    @Override
    public Optional<FestivalBooth> findById(Long boothId) {
        return jpaRepository.findById(boothId);
    }

    @Override
    public Optional<FestivalBooth> findByFestivalIdAndPublicId(
            Long festivalId,
            UUID publicId
    ) {
        return jpaRepository.findByFestivalIdAndPublicId(festivalId, publicId);
    }

    @Override
    public Optional<FestivalBooth> findByFestivalIdAndPublicIdForUpdate(
            Long festivalId,
            UUID publicId
    ) {
        return jpaRepository.findByFestivalIdAndPublicIdForUpdate(
                festivalId,
                publicId
        );
    }
}
