package com.example.demoadmin.map.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MapStoragePathTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("배치도 저장 경로를 생성한다")
        void success_Of() {
            // given
            String value = "images/김밥축제_지적편집도.png";

            // when
            MapStoragePath storagePath = MapStoragePath.of(value);

            // then
            assertThat(storagePath.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("배치도 저장 경로는 1000자까지 허용한다")
        void success_Of_MaxLengthBoundary() {
            // given
            String value = "a".repeat(1000);

            // when
            MapStoragePath storagePath = MapStoragePath.of(value);

            // then
            assertThat(storagePath.getValue()).hasSize(1000);
        }

        @Test
        @DisplayName("배치도 저장 경로가 비어 있으면 생성할 수 없다")
        void fail_Of_CustomException_Blank() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> MapStoragePath.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
