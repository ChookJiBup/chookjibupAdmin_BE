package com.example.chookjibupadmin.festival.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FestivalNameTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("JPA 기본 생성자로 생성할 수 있다")
        void success_Constructor_ForJpa() {
            // given

            // when
            FestivalName name = new FestivalName();

            // then
            assertThat(name.getValue()).isNull();
        }
    }

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("축제명은 앞뒤 공백을 제거한다")
        void success_Of_Normalized() {
            // given
            String value = " 마포 축제 ";

            // when
            FestivalName name = FestivalName.of(value);

            // then
            assertThat(name.getValue()).isEqualTo("마포 축제");
        }

        @Test
        @DisplayName("축제명은 최소 길이 경계값이면 생성한다")
        void success_Of_MinLengthBoundary() {
            // given
            String value = "축제";

            // when
            FestivalName name = FestivalName.of(value);

            // then
            assertThat(name.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("축제명은 최대 길이 경계값이면 생성한다")
        void success_Of_MaxLengthBoundary() {
            // given
            String value = "가".repeat(100);

            // when
            FestivalName name = FestivalName.of(value);

            // then
            assertThat(name.getValue()).hasSize(100);
        }

        @Test
        @DisplayName("축제명이 null이면 생성할 수 없다")
        void fail_Of_Null_CustomException() {
            // given
            String value = null;

            // when & then
            assertThatThrownBy(() -> FestivalName.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("축제명은 최소 길이보다 짧으면 생성할 수 없다")
        void fail_Of_UnderMinLength_CustomException() {
            // given
            String value = "축";

            // when & then
            assertThatThrownBy(() -> FestivalName.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("축제명은 최대 길이보다 길면 생성할 수 없다")
        void fail_Of_OverMaxLength_CustomException() {
            // given
            String value = "가".repeat(101);

            // when & then
            assertThatThrownBy(() -> FestivalName.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
