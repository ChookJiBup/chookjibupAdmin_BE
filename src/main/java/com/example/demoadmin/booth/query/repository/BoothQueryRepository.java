package com.example.demoadmin.booth.query.repository;

import com.example.demoadmin.booth.query.application.dto.BoothQueueLineView;
import com.example.demoadmin.booth.query.application.dto.BoothView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoothQueryRepository {

    List<BoothView> findAllByFestivalId(Long festivalId);

    Optional<BoothView> findByFestivalIdAndPublicId(
            Long festivalId,
            UUID boothId
    );

    List<BoothQueueLineView> findQueueLinesByFestivalIdAndBoothPublicId(
            Long festivalId,
            UUID boothId
    );
}
