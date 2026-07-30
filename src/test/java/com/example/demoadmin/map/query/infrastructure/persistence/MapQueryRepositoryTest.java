package com.example.demoadmin.map.query.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.map.command.application.FestivalMapService;
import com.example.demoadmin.map.command.application.MapObjectService;
import com.example.demoadmin.map.command.domain.FestivalMap;
import com.example.demoadmin.map.command.domain.GeometryType;
import com.example.demoadmin.map.command.domain.MapObject;
import com.example.demoadmin.map.command.domain.MapObjectType;
import com.example.demoadmin.map.command.domain.MapStorageType;
import com.example.demoadmin.map.command.domain.vo.ConfidenceScore;
import com.example.demoadmin.map.command.domain.vo.GeometryData;
import com.example.demoadmin.map.command.domain.vo.MapFileName;
import com.example.demoadmin.map.command.domain.vo.MapObjectName;
import com.example.demoadmin.map.command.domain.vo.MapStoragePath;
import com.example.demoadmin.map.query.repository.MapQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MapQueryRepositoryTest {

    @Autowired
    private MapQueryRepository mapQueryRepository;

    @Autowired
    private FestivalMapService festivalMapService;

    @Autowired
    private MapObjectService mapObjectService;

    @Nested
    @DisplayName("findObjectsByFestivalIdAndMapPublicId")
    class FindObjectsByFestivalIdAndMapPublicId {

        @Test
        @DisplayName("다른 축제 객체를 제외하고 Projection으로 조회한다")
        void success_FindObjectsByFestivalIdAndMapPublicId() {
            // given
            FestivalMap targetMap = festivalMapService.save(festivalMap(1L));
            FestivalMap otherMap = festivalMapService.save(festivalMap(2L));
            mapObjectService.save(mapObject(targetMap.getId(), "김밥 부스"));
            mapObjectService.save(mapObject(otherMap.getId(), "다른 부스"));

            // when
            var result = mapQueryRepository
                    .findObjectsByFestivalIdAndMapPublicId(
                            1L,
                            targetMap.getPublicId()
                    );

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo("김밥 부스");
            assertThat(result.getFirst().geometryData())
                    .contains("\"type\":\"RECTANGLE\"");
        }
    }

    private FestivalMap festivalMap(Long festivalId) {
        FestivalMap festivalMap = FestivalMap.create(
                festivalId,
                MapFileName.of("김밥축제_지적편집도.png"),
                MapStorageType.TEST_RESOURCE,
                MapStoragePath.of("images/김밥축제_지적편집도.png"),
                1745,
                1577
        );
        festivalMap.markAnalyzed();
        return festivalMap;
    }

    private MapObject mapObject(
            Long festivalMapId,
            String name
    ) {
        return MapObject.createAiGenerated(
                festivalMapId,
                1L,
                MapObjectType.FOOD_BOOTH,
                MapObjectName.of(name),
                GeometryType.RECTANGLE,
                GeometryData.of(
                        "{\"type\":\"RECTANGLE\",\"x\":0.31,\"y\":0.22,"
                                + "\"width\":0.08,\"height\":0.05}"
                ),
                ConfidenceScore.of(0.82)
        );
    }
}
