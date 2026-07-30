package com.example.demoadmin.api.map.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.map.command.domain.FestivalMapStatus;
import com.example.demoadmin.map.command.domain.GeometryType;
import com.example.demoadmin.map.command.domain.MapDetectionSource;
import com.example.demoadmin.map.command.domain.MapObjectType;
import com.example.demoadmin.map.command.domain.MapReviewStatus;
import com.example.demoadmin.map.query.application.dto.FestivalMapObjectsView;
import com.example.demoadmin.map.query.application.dto.FestivalMapView;
import com.example.demoadmin.map.query.application.dto.MapObjectView;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FestivalMapObjectsResponseTest {

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("문자열 geometry를 프런트가 사용할 JSON 객체로 변환한다")
        void success_From() {
            // given
            UUID mapId = UUID.randomUUID();
            FestivalMapObjectsView view = new FestivalMapObjectsView(
                    new FestivalMapView(
                            mapId,
                            FestivalMapStatus.ANALYZED,
                            1745,
                            1577
                    ),
                    List.of(new MapObjectView(
                            UUID.randomUUID(),
                            MapObjectType.FOOD_BOOTH,
                            "김밥 부스",
                            GeometryType.RECTANGLE,
                            "{\"type\":\"RECTANGLE\",\"x\":0.31,\"y\":0.22,"
                                    + "\"width\":0.08,\"height\":0.05}",
                            0.82,
                            MapReviewStatus.REVIEW_REQUIRED,
                            MapDetectionSource.AI_GENERATED
                    ))
            );

            // when
            FestivalMapObjectsResponse result =
                    FestivalMapObjectsResponse.from(view);

            // then
            assertThat(result.mapId()).isEqualTo(mapId);
            assertThat(result.objects().getFirst().geometry().get("type"))
                    .isEqualTo("RECTANGLE");
            assertThat(result.objects().getFirst().geometry().get("x"))
                    .isEqualTo(0.31);
        }
    }
}
