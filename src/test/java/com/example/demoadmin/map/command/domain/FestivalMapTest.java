package com.example.demoadmin.map.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.domain.vo.MapFileName;
import com.example.demoadmin.map.command.domain.vo.MapStoragePath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FestivalMapTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("테스트 리소스 배치도를 생성한다")
        void success_Create() {
            // given
            Long festivalId = 1L;

            // when
            FestivalMap festivalMap = festivalMap(festivalId);

            // then
            assertThat(festivalMap.getPublicId()).isNotNull();
            assertThat(festivalMap.getFestivalId()).isEqualTo(festivalId);
            assertThat(festivalMap.getStatus()).isEqualTo(FestivalMapStatus.DRAFT);
        }

        @Test
        @DisplayName("이미지 너비는 1 이상이어야 한다")
        void fail_Create_CustomException_WidthBoundary() {
            // given
            int width = 0;

            // when & then
            assertThatThrownBy(() -> FestivalMap.create(
                    1L,
                    MapFileName.of("map.png"),
                    MapStorageType.TEST_RESOURCE,
                    MapStoragePath.of("images/map.png"),
                    width,
                    100
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("분석된 배치도를 확정한다")
        void success_Confirm() {
            // given
            FestivalMap festivalMap = festivalMap(1L);
            festivalMap.markAnalyzed();

            // when
            festivalMap.confirm();

            // then
            assertThat(festivalMap.getStatus()).isEqualTo(FestivalMapStatus.CONFIRMED);
        }

        @Test
        @DisplayName("분석 전 배치도는 확정할 수 없다")
        void fail_Confirm_CustomException() {
            // given
            FestivalMap festivalMap = festivalMap(1L);

            // when & then
            assertThatThrownBy(festivalMap::confirm)
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_MAP_STATUS_INVALID.getMessage());
        }
    }

    private FestivalMap festivalMap(Long festivalId) {
        return FestivalMap.create(
                festivalId,
                MapFileName.of("김밥축제_지적편집도.png"),
                MapStorageType.TEST_RESOURCE,
                MapStoragePath.of("images/김밥축제_지적편집도.png"),
                1745,
                1577
        );
    }
}
