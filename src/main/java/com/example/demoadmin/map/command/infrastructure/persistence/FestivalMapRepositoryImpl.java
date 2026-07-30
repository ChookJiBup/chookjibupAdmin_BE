package com.example.demoadmin.map.command.infrastructure.persistence;

import com.example.demoadmin.map.command.domain.FestivalMap;
import com.example.demoadmin.map.command.domain.FestivalMapRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FestivalMapRepositoryImpl implements FestivalMapRepository {

    private final FestivalMapJpaRepository jpaRepository;

    @Override
    public FestivalMap save(FestivalMap festivalMap) {
        return jpaRepository.save(festivalMap);
    }

    @Override
    public Optional<FestivalMap> findById(Long festivalMapId) {
        return jpaRepository.findById(festivalMapId);
    }

    @Override
    public Optional<FestivalMap> findByFestivalIdAndPublicId(
            Long festivalId,
            UUID publicId
    ) {
        return jpaRepository.findByFestivalIdAndPublicId(festivalId, publicId);
    }
}
