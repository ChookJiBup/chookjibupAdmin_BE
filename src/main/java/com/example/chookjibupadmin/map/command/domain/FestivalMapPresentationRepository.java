package com.example.chookjibupadmin.map.command.domain;

import java.util.Optional;

public interface FestivalMapPresentationRepository {

    FestivalMapPresentation save(FestivalMapPresentation presentation);

    Optional<FestivalMapPresentation> findByMapId(Long mapId);

    Optional<FestivalMapPresentation> findByMapIdForUpdate(Long mapId);

    void deleteByMapIdIn(Iterable<Long> mapIds);
}
