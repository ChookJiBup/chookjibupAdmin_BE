package com.example.chookjibupadmin.festival.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FestivalDescriptionTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("JPA 기본 생성자로 생성할 수 있다")
        void success_Constructor_ForJpa() {
            // given

            // when
            FestivalDescription description = new FestivalDescription();

            // then
            assertThat(description.getValue()).isNull();
        }
    }

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("축제 설명은 앞뒤 공백을 제거한다")
        void success_Of_Normalized() {
            // given
            String value = " 지역 대표 축제 ";

            // when
            FestivalDescription description = FestivalDescription.of(value);

            // then
            assertThat(description.getValue()).isEqualTo("지역 대표 축제");
        }

        @Test
        @DisplayName("축제 설명은 최대 길이 경계값이면 생성한다")
        void success_Of_MaxLengthBoundary() {
            // given
            String value = "가".repeat(1000);

            // when
            FestivalDescription description = FestivalDescription.of(value);

            // then
            assertThat(description.getValue()).hasSize(1000);
        }

        @Test
        @DisplayName("축제 설명이 null이면 생성할 수 없다")
        void fail_Of_Null_CustomException() {
            // given
            String value = null;

            // when & then
            assertThatThrownBy(() -> FestivalDescription.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("축제 설명이 공백이면 생성할 수 없다")
        void fail_Of_Blank_CustomException() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> FestivalDescription.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("축제 설명은 최대 길이보다 길면 생성할 수 없다")
        void fail_Of_OverMaxLength_CustomException() {
            // given
            String value = "가".repeat(1001);

            // when & then
            assertThatThrownBy(() -> FestivalDescription.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
