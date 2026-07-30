package com.example.demoadmin.map.query.infrastructure.persistence;

import com.example.demoadmin.map.command.domain.QFestivalMap;
import com.example.demoadmin.map.command.domain.QMapObject;
import com.example.demoadmin.map.query.application.dto.FestivalMapView;
import com.example.demoadmin.map.query.application.dto.MapObjectView;
import com.example.demoadmin.map.query.repository.MapQueryRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * QueryDSL Projection으로 배치도 조회를 수행한다.
 */
@Repository
@RequiredArgsConstructor
public class MapQueryRepositoryImpl implements MapQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<FestivalMapView> findMapByFestivalIdAndPublicId(
            Long festivalId,
            UUID mapId
    ) {
        QFestivalMap festivalMap = QFestivalMap.festivalMap;

        FestivalMapView result = queryFactory
                .select(Projections.constructor(
                        FestivalMapView.class,
                        festivalMap.publicId,
                        festivalMap.status,
                        festivalMap.width,
                        festivalMap.height
                ))
                .from(festivalMap)
                .where(
                        festivalMap.festivalId.eq(festivalId),
                        festivalMap.publicId.eq(mapId)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<MapObjectView> findObjectsByFestivalIdAndMapPublicId(
            Long festivalId,
            UUID mapId
    ) {
        QFestivalMap festivalMap = QFestivalMap.festivalMap;
        QMapObject mapObject = QMapObject.mapObject;

        return queryFactory
                .select(Projections.constructor(
                        MapObjectView.class,
                        mapObject.publicId,
                        mapObject.type,
                        mapObject.name.value,
                        mapObject.geometryType,
                        mapObject.geometryData.value,
                        mapObject.confidence.value,
                        mapObject.reviewStatus,
                        mapObject.source
                ))
                .from(mapObject)
                .join(festivalMap).on(festivalMap.id.eq(mapObject.festivalMapId))
                .where(
                        festivalMap.festivalId.eq(festivalId),
                        festivalMap.publicId.eq(mapId)
                )
                .orderBy(mapObject.id.asc())
                .fetch();
    }
}
