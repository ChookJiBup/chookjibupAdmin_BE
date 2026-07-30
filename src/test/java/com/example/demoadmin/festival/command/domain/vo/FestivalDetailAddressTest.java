package com.example.demoadmin.festival.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FestivalDetailAddressTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("JPA 기본 생성자로 생성할 수 있다")
        void success_Constructor_ForJpa() {
            // given

            // when
            FestivalDetailAddress detailAddress =
                    new FestivalDetailAddress();

            // then
            assertThat(detailAddress.getValue()).isNull();
        }
    }

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("상세주소는 앞뒤 공백을 제거한다")
        void success_Of_Normalized() {
            // given
            String value = " 광주비엔날레 전시관 ";

            // when
            FestivalDetailAddress detailAddress =
                    FestivalDetailAddress.of(value);

            // then
            assertThat(detailAddress.getValue())
                    .isEqualTo("광주비엔날레 전시관");
        }

        @Test
        @DisplayName("상세주소가 null이면 빈 값으로 생성한다")
        void success_Of_NullBoundary() {
            // given
            String value = null;

            // when
            FestivalDetailAddress detailAddress =
                    FestivalDetailAddress.of(value);

            // then
            assertThat(detailAddress.getValue()).isNull();
        }

        @Test
        @DisplayName("상세주소가 공백이면 빈 값으로 생성한다")
        void success_Of_BlankBoundary() {
            // given
            String value = " ";

            // when
            FestivalDetailAddress detailAddress =
                    FestivalDetailAddress.of(value);

            // then
            assertThat(detailAddress.getValue()).isNull();
        }

        @Test
        @DisplayName("상세주소는 최대 길이 경계값이면 생성한다")
        void success_Of_MaxLengthBoundary() {
            // given
            String value = "가".repeat(100);

            // when
            FestivalDetailAddress detailAddress =
                    FestivalDetailAddress.of(value);

            // then
            assertThat(detailAddress.getValue()).hasSize(100);
        }

        @Test
        @DisplayName("상세주소가 최대 길이보다 길면 생성할 수 없다")
        void fail_Of_OverMaxLength_CustomException() {
            // given
            String value = "가".repeat(101);

            // when & then
            assertThatThrownBy(() -> FestivalDetailAddress.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
