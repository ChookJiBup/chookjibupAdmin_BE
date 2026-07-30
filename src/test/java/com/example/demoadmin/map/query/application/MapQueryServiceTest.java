package com.example.demoadmin.map.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.domain.FestivalMapStatus;
import com.example.demoadmin.map.query.application.dto.FestivalMapView;
import com.example.demoadmin.map.query.repository.MapQueryRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapQueryServiceTest {

    @InjectMocks
    private MapQueryService mapQueryService;

    @Mock
    private MapQueryRepository mapQueryRepository;

    @Nested
    @DisplayName("getMap")
    class GetMap {

        @Test
        @DisplayName("축제와 배치도 UUID로 배치도 정보를 반환한다")
        void success_GetMap() {
            // given
            UUID mapId = UUID.randomUUID();
            FestivalMapView expected = new FestivalMapView(
                    mapId,
                    FestivalMapStatus.ANALYZED,
                    1745,
                    1577
            );
            given(mapQueryRepository.findMapByFestivalIdAndPublicId(
                    1L,
                    mapId
            )).willReturn(Optional.of(expected));

            // when
            FestivalMapView result = mapQueryService.getMap(1L, mapId);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("배치도가 없으면 축제 배치도 없음 예외를 던진다")
        void fail_GetMap_FestivalMapNotFound() {
            // given
            UUID mapId = UUID.randomUUID();
            given(mapQueryRepository.findMapByFestivalIdAndPublicId(
                    1L,
                    mapId
            )).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> mapQueryService.getMap(1L, mapId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_MAP_NOT_FOUND.getMessage());
        }
    }
}
