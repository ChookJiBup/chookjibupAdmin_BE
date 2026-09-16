package com.example.chookjibupadmin.booth.command.infrastructure.persistence;

import com.example.chookjibupadmin.booth.command.domain.BoothQueuePlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothQueuePlanJpaRepository extends JpaRepository<BoothQueuePlan, Long> {
    Optional<BoothQueuePlan> findByBoothId(Long boothId);
}
