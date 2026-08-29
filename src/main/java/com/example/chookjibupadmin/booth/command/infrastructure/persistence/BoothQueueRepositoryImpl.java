package com.example.chookjibupadmin.booth.command.infrastructure.persistence;

import com.example.chookjibupadmin.booth.command.domain.BoothQueue;
import com.example.chookjibupadmin.booth.command.domain.BoothQueueRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BoothQueueRepositoryImpl implements BoothQueueRepository {

    private final BoothQueueJpaRepository jpaRepository;

    @Override
    public BoothQueue save(BoothQueue queue) {
        return jpaRepository.save(queue);
    }

    @Override
    public Optional<BoothQueue> findByPublicId(UUID publicId) {
        return jpaRepository.findByPublicId(publicId);
    }

    @Override
    public Optional<BoothQueue> findByBoothId(Long boothId) {
        return jpaRepository.findByBoothId(boothId);
    }

    @Override
    public List<BoothQueue> findAllByFestivalId(Long festivalId) {
        return jpaRepository.findAllByFestivalIdOrderByIdAsc(festivalId);
    }

    @Override
    public List<BoothQueue> findAllByBoothIdIn(Collection<Long> boothIds) {
        if (boothIds == null || boothIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllByBoothIdIn(boothIds);
    }
}
