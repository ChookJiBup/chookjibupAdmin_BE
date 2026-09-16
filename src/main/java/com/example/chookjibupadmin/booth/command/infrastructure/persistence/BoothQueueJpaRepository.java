package com.example.chookjibupadmin.booth.command.infrastructure.persistence;

import com.example.chookjibupadmin.booth.command.domain.BoothQueue;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoothQueueJpaRepository extends JpaRepository<BoothQueue, Long> {

    Optional<BoothQueue> findByPublicId(UUID publicId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select q from BoothQueue q where q.publicId = :id")
    Optional<BoothQueue> findByPublicIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);

    Optional<BoothQueue> findByBoothId(Long boothId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select q from BoothQueue q where q.boothId = :id")
    Optional<BoothQueue> findByBoothIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    List<BoothQueue> findAllByFestivalIdOrderByIdAsc(Long festivalId);

    List<BoothQueue> findAllByBoothIdIn(Collection<Long> boothIds);
}
