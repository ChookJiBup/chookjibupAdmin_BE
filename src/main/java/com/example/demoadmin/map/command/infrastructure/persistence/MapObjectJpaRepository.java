package com.example.demoadmin.map.command.infrastructure.persistence;

import com.example.demoadmin.map.command.domain.MapObject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface MapObjectJpaRepository extends JpaRepository<MapObject, Long> {

    List<MapObject> findByFestivalMapId(Long festivalMapId);
}
