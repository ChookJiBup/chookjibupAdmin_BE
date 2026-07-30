package com.example.demoadmin.booth.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BoothNameTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("부스명을 생성한다")
        void success_Of() {
            // given
            String value = "푸드 부스";

            // when
            BoothName name = BoothName.of(value);

            // then
            assertThat(name.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("부스명은 100자까지 허용한다")
        void success_Of_MaxLength() {
            // given
            String value = "가".repeat(100);

            // when
            BoothName name = BoothName.of(value);

            // then
            assertThat(name.getValue()).hasSize(100);
        }

        @Test
        @DisplayName("부스명이 비어 있으면 생성할 수 없다")
        void fail_Of_CustomException_Blank() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> BoothName.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
