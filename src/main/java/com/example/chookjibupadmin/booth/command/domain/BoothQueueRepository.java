package com.example.chookjibupadmin.booth.command.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoothQueueRepository {

    BoothQueue save(BoothQueue queue);

    Optional<BoothQueue> findByPublicId(UUID publicId);

    Optional<BoothQueue> findByBoothId(Long boothId);

    List<BoothQueue> findAllByFestivalId(Long festivalId);

    List<BoothQueue> findAllByBoothIdIn(Collection<Long> boothIds);
}
