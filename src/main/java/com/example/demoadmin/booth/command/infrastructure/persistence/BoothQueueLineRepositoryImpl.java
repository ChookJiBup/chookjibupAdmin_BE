package com.example.demoadmin.booth.command.infrastructure.persistence;

import com.example.demoadmin.booth.command.domain.BoothQueueLine;
import com.example.demoadmin.booth.command.domain.BoothQueueLineRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BoothQueueLineRepositoryImpl implements BoothQueueLineRepository {

    private final BoothQueueLineJpaRepository jpaRepository;

    @Override
    public BoothQueueLine save(BoothQueueLine queueLine) {
        return jpaRepository.save(queueLine);
    }

    @Override
    public Optional<BoothQueueLine> findByBoothIdAndPublicId(
            Long boothId,
            UUID publicId
    ) {
        return jpaRepository.findByBoothIdAndPublicId(boothId, publicId);
    }

    @Override
    public boolean existsByBoothIdAndLineOrder(
            Long boothId,
            int lineOrder
    ) {
        return jpaRepository.existsByBoothIdAndLineOrder(boothId, lineOrder);
    }

    @Override
    public boolean existsByBoothIdAndLineOrderAndIdNot(
            Long boothId,
            int lineOrder,
            Long id
    ) {
        return jpaRepository.existsByBoothIdAndLineOrderAndIdNot(
                boothId,
                lineOrder,
                id
        );
    }
}
