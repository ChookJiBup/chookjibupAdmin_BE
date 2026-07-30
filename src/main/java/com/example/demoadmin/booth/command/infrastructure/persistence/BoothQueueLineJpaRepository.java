package com.example.demoadmin.booth.command.infrastructure.persistence;

import com.example.demoadmin.booth.command.domain.BoothQueueLine;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BoothQueueLineJpaRepository extends JpaRepository<BoothQueueLine, Long> {

    Optional<BoothQueueLine> findByBoothIdAndPublicId(
            Long boothId,
            UUID publicId
    );

    boolean existsByBoothIdAndLineOrder(
            Long boothId,
            int lineOrder
    );

    boolean existsByBoothIdAndLineOrderAndIdNot(
            Long boothId,
            int lineOrder,
            Long id
    );
}
