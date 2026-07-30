package com.example.demoadmin.map.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.map.command.domain.GeometryType;
import com.example.demoadmin.map.command.domain.MapObject;
import com.example.demoadmin.map.command.domain.MapObjectRepository;
import com.example.demoadmin.map.command.domain.MapObjectType;
import com.example.demoadmin.map.command.domain.vo.ConfidenceScore;
import com.example.demoadmin.map.command.domain.vo.GeometryData;
import com.example.demoadmin.map.command.domain.vo.MapObjectName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MapObjectRepositoryTest {

    @Autowired
    private MapObjectRepository mapObjectRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("배치도 객체를 저장한다")
        void success_Save() {
            // given
            MapObject mapObject = mapObject(1L);

            // when
            MapObject saved = mapObjectRepository.save(mapObject);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getPublicId()).isNotNull();
            assertThat(saved.getNameValue()).isEqualTo("김밥 부스");
        }
    }

    @Nested
    @DisplayName("findByFestivalMapId")
    class FindByFestivalMapId {

        @Test
        @DisplayName("배치도 ID로 객체 목록을 조회한다")
        void success_FindByFestivalMapId() {
            // given
            mapObjectRepository.save(mapObject(1L));
            mapObjectRepository.save(mapObject(2L));

            // when
            var result = mapObjectRepository.findByFestivalMapId(1L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getFestivalMapId()).isEqualTo(1L);
        }
    }

    private MapObject mapObject(Long festivalMapId) {
        return MapObject.createAiGenerated(
                festivalMapId,
                1L,
                MapObjectType.FOOD_BOOTH,
                MapObjectName.of("김밥 부스"),
                GeometryType.RECTANGLE,
                GeometryData.of(
                        "{\"type\":\"RECTANGLE\",\"x\":0.31,\"y\":0.22,"
                                + "\"width\":0.08,\"height\":0.05}"
                ),
                ConfidenceScore.of(0.82)
        );
    }
}
