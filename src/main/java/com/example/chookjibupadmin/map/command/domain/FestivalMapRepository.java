package com.example.chookjibupadmin.map.command.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * 축제 배치도 저장소 계약이다.
 */
public interface FestivalMapRepository {

    FestivalMap save(FestivalMap festivalMap);

    Optional<FestivalMap> findById(Long id);

    Optional<FestivalMap> findByPublicId(UUID publicId);

    Optional<FestivalMap> findByPublicIdForUpdate(UUID publicId);

    boolean existsByLocationId(Long locationId);
}
