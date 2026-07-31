package com.example.chookjibupadmin.festival.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FestivalAddressTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("JPA 기본 생성자로 생성할 수 있다")
        void success_Constructor_ForJpa() {
            // given

            // when
            FestivalAddress address = new FestivalAddress();

            // then
            assertThat(address.getValue()).isNull();
        }
    }

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("축제 주소는 앞뒤 공백을 제거한다")
        void success_Of_Normalized() {
            // given
            String value = " 서울시 마포구 ";

            // when
            FestivalAddress address = FestivalAddress.of(value);

            // then
            assertThat(address.getValue()).isEqualTo("서울시 마포구");
        }

        @Test
        @DisplayName("축제 주소는 최소 길이 경계값이면 생성한다")
        void success_Of_MinLengthBoundary() {
            // given
            String value = "서울";

            // when
            FestivalAddress address = FestivalAddress.of(value);

            // then
            assertThat(address.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("축제 주소는 최대 길이 경계값이면 생성한다")
        void success_Of_MaxLengthBoundary() {
            // given
            String value = "가".repeat(255);

            // when
            FestivalAddress address = FestivalAddress.of(value);

            // then
            assertThat(address.getValue()).hasSize(255);
        }

        @Test
        @DisplayName("축제 주소가 공백이면 생성할 수 없다")
        void fail_Of_Blank_CustomException() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> FestivalAddress.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("축제 주소는 최소 길이보다 짧으면 생성할 수 없다")
        void fail_Of_UnderMinLength_CustomException() {
            // given
            String value = "서";

            // when & then
            assertThatThrownBy(() -> FestivalAddress.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("축제 주소는 최대 길이보다 길면 생성할 수 없다")
        void fail_Of_OverMaxLength_CustomException() {
            // given
            String value = "가".repeat(256);

            // when & then
            assertThatThrownBy(() -> FestivalAddress.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
