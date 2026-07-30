package com.example.demoadmin.booth.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BoothDescriptionTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("부스 설명을 생성한다")
        void success_Of() {
            // given
            String value = "대표 먹거리 부스";

            // when
            BoothDescription description = BoothDescription.of(value);

            // then
            assertThat(description.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("부스 설명은 없으면 빈 문자열로 관리한다")
        void success_Of_Null() {
            // given
            String value = null;

            // when
            BoothDescription description = BoothDescription.of(value);

            // then
            assertThat(description.getValue()).isEmpty();
        }

        @Test
        @DisplayName("부스 설명은 1000자까지 허용한다")
        void success_Of_MaxLength() {
            // given
            String value = "가".repeat(1000);

            // when
            BoothDescription description = BoothDescription.of(value);

            // then
            assertThat(description.getValue()).hasSize(1000);
        }

        @Test
        @DisplayName("부스 설명이 1000자를 넘으면 생성할 수 없다")
        void fail_Of_CustomException_OverMaxLength() {
            // given
            String value = "가".repeat(1001);

            // when & then
            assertThatThrownBy(() -> BoothDescription.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
