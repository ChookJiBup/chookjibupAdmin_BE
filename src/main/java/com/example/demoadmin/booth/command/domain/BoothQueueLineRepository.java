package com.example.demoadmin.booth.command.domain;

import java.util.Optional;
import java.util.UUID;

public interface BoothQueueLineRepository {

    BoothQueueLine save(BoothQueueLine queueLine);

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
