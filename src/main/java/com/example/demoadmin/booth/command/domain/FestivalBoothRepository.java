package com.example.demoadmin.booth.command.domain;

import java.util.Optional;
import java.util.UUID;

public interface FestivalBoothRepository {

    FestivalBooth save(FestivalBooth booth);

    Optional<FestivalBooth> findById(Long boothId);

    Optional<FestivalBooth> findByFestivalIdAndPublicId(
            Long festivalId,
            UUID publicId
    );

    Optional<FestivalBooth> findByFestivalIdAndPublicIdForUpdate(
            Long festivalId,
            UUID publicId
    );
}
