package com.example.demoadmin.map.command.infrastructure.persistence;

import com.example.demoadmin.map.command.domain.MapObject;
import com.example.demoadmin.map.command.domain.MapObjectRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MapObjectRepositoryImpl implements MapObjectRepository {

    private final MapObjectJpaRepository jpaRepository;

    @Override
    public MapObject save(MapObject mapObject) {
        return jpaRepository.save(mapObject);
    }

    @Override
    public List<MapObject> saveAll(List<MapObject> mapObjects) {
        return jpaRepository.saveAll(mapObjects);
    }

    @Override
    public List<MapObject> findByFestivalMapId(Long festivalMapId) {
        return jpaRepository.findByFestivalMapId(festivalMapId);
    }
}
