package com.example.demoadmin.map.command.application;

import com.example.demoadmin.map.command.domain.MapObject;
import com.example.demoadmin.map.command.domain.MapObjectRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배치도 객체 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapObjectService {

    private final MapObjectRepository mapObjectRepository;

    @Transactional
    public MapObject save(MapObject mapObject) {
        return mapObjectRepository.save(mapObject);
    }

    @Transactional
    public List<MapObject> saveAll(List<MapObject> mapObjects) {
        return mapObjectRepository.saveAll(mapObjects);
    }

    public List<MapObject> findByFestivalMapId(Long festivalMapId) {
        return mapObjectRepository.findByFestivalMapId(festivalMapId);
    }
}
