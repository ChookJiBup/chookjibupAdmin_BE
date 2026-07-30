package com.example.demoadmin.map.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.domain.vo.ConfidenceScore;
import com.example.demoadmin.map.command.domain.vo.GeometryData;
import com.example.demoadmin.map.command.domain.vo.MapObjectName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MapObjectTest {

    @Nested
    @DisplayName("createAiGenerated")
    class CreateAiGenerated {

        @Test
        @DisplayName("AI 분석 지도 객체를 검수 대기 상태로 생성한다")
        void success_CreateAiGenerated() {
            // given
            Long festivalMapId = 1L;

            // when
            MapObject mapObject = mapObject(festivalMapId);

            // then
            assertThat(mapObject.getPublicId()).isNotNull();
            assertThat(mapObject.getFestivalMapId()).isEqualTo(festivalMapId);
            assertThat(mapObject.getReviewStatus()).isEqualTo(MapReviewStatus.REVIEW_REQUIRED);
            assertThat(mapObject.getSource()).isEqualTo(MapDetectionSource.AI_GENERATED);
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("검수 대기 객체를 확정한다")
        void success_Confirm() {
            // given
            MapObject mapObject = mapObject(1L);

            // when
            mapObject.confirm();

            // then
            assertThat(mapObject.getReviewStatus()).isEqualTo(MapReviewStatus.CONFIRMED);
        }

        @Test
        @DisplayName("반려된 객체는 확정할 수 없다")
        void fail_Confirm_CustomException() {
            // given
            MapObject mapObject = mapObject(1L);
            mapObject.reject();

            // when & then
            assertThatThrownBy(mapObject::confirm)
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.MAP_OBJECT_REVIEW_STATUS_INVALID.getMessage());
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
