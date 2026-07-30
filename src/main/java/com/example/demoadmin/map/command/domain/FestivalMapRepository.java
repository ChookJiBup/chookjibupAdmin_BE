package com.example.demoadmin.map.command.domain;

import java.util.Optional;
import java.util.UUID;

public interface FestivalMapRepository {

    FestivalMap save(FestivalMap festivalMap);

    Optional<FestivalMap> findById(Long festivalMapId);

    Optional<FestivalMap> findByFestivalIdAndPublicId(
            Long festivalId,
            UUID publicId
    );
}
