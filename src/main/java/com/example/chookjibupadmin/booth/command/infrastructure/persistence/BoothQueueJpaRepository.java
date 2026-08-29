package com.example.chookjibupadmin.booth.command.infrastructure.persistence;

import com.example.chookjibupadmin.booth.command.domain.BoothQueue;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothQueueJpaRepository extends JpaRepository<BoothQueue, Long> {

    Optional<BoothQueue> findByPublicId(UUID publicId);

    Optional<BoothQueue> findByBoothId(Long boothId);

    List<BoothQueue> findAllByFestivalIdOrderByIdAsc(Long festivalId);

    List<BoothQueue> findAllByBoothIdIn(Collection<Long> boothIds);
}
