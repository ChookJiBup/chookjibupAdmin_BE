package com.example.chookjibupadmin.map.command.infrastructure.persistence;

import com.example.chookjibupadmin.map.command.domain.FestivalMapPresentation;
import com.example.chookjibupadmin.map.command.domain.FestivalMapPresentationRepository;
import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FestivalMapPresentationRepositoryImpl
        implements FestivalMapPresentationRepository {

    private final FestivalMapPresentationJpaRepository jpaRepository;

    @Override
    public FestivalMapPresentation save(FestivalMapPresentation presentation) {
        return jpaRepository.save(presentation);
    }

    @Override
    public Optional<FestivalMapPresentation> findByMapId(Long mapId) {
        return jpaRepository.findByMapId(mapId);
    }

    @Override
    public Optional<FestivalMapPresentation> findByMapIdForUpdate(Long mapId) {
        return jpaRepository.findByMapIdForUpdate(mapId);
    }

    @Override
    public void deleteByMapIdIn(Iterable<Long> mapIds) {
        ArrayList<Long> ids = new ArrayList<>();
        mapIds.forEach(ids::add);
        if (!ids.isEmpty()) {
            jpaRepository.deleteByMapIdIn(ids);
        }
    }
}
