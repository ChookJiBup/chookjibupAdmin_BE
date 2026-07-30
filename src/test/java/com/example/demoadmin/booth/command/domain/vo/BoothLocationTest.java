package com.example.demoadmin.booth.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BoothLocationTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("부스 위치를 생성한다")
        void success_Of() {
            // given
            String value = "A-1";

            // when
            BoothLocation location = BoothLocation.of(value);

            // then
            assertThat(location.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("부스 위치는 255자까지 허용한다")
        void success_Of_MaxLength() {
            // given
            String value = "가".repeat(255);

            // when
            BoothLocation location = BoothLocation.of(value);

            // then
            assertThat(location.getValue()).hasSize(255);
        }

        @Test
        @DisplayName("부스 위치가 비어 있으면 생성할 수 없다")
        void fail_Of_CustomException_Blank() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> BoothLocation.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
