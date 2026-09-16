package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.booth.command.domain.BoothQueuePlan;
import com.example.chookjibupadmin.booth.command.domain.BoothQueuePlanRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사전 계획 저장소 래퍼이다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoothQueuePlanService {
    private final BoothQueuePlanRepository repository;
    public Optional<BoothQueuePlan> findByBoothId(Long boothId) { return repository.findByBoothId(boothId); }
    @Transactional
    public BoothQueuePlan save(BoothQueuePlan plan) { return repository.save(plan); }
}
