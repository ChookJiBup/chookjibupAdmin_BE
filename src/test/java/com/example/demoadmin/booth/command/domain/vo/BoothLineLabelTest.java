package com.example.demoadmin.booth.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BoothLineLabelTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("대기 라인명을 생성한다")
        void success_Of() {
            // given
            String value = "1번 라인";

            // when
            BoothLineLabel label = BoothLineLabel.of(value);

            // then
            assertThat(label.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("대기 라인명은 100자까지 허용한다")
        void success_Of_MaxLength() {
            // given
            String value = "가".repeat(100);

            // when
            BoothLineLabel label = BoothLineLabel.of(value);

            // then
            assertThat(label.getValue()).hasSize(100);
        }

        @Test
        @DisplayName("대기 라인명이 비어 있으면 생성할 수 없다")
        void fail_Of_CustomException_Blank() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> BoothLineLabel.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
