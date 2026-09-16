package com.example.chookjibupadmin.booth.command.infrastructure.persistence;

import com.example.chookjibupadmin.booth.command.domain.BoothQueuePlan;
import com.example.chookjibupadmin.booth.command.domain.BoothQueuePlanRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BoothQueuePlanRepositoryImpl implements BoothQueuePlanRepository {
    private final BoothQueuePlanJpaRepository jpaRepository;
    public Optional<BoothQueuePlan> findByBoothId(Long boothId) { return jpaRepository.findByBoothId(boothId); }
    public BoothQueuePlan save(BoothQueuePlan plan) { return jpaRepository.save(plan); }
}
