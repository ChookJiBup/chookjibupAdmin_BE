package com.example.chookjibupadmin.map.command.infrastructure.persistence;

import com.example.chookjibupadmin.map.command.domain.FestivalMap;
import com.example.chookjibupadmin.map.command.domain.FestivalMapRepository;
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
    public Optional<FestivalMap> findByPublicId(UUID publicId) {
        return jpaRepository.findByPublicId(publicId);
    }
}
