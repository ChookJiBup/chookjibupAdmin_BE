package com.example.chookjibupadmin.admin.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminRankTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("JPA 기본 생성자로 생성할 수 있다")
        void success_Constructor_ForJpa() {
            // given

            // when
            AdminRank rank = new AdminRank();

            // then
            assertThat(rank.getValue()).isNull();
        }
    }

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("직급 앞뒤 공백을 제거한다")
        void success_Of_Normalized() {
            // given
            String value = " 주무관 ";

            // when
            AdminRank rank = AdminRank.of(value);

            // then
            assertThat(rank.getValue()).isEqualTo("주무관");
        }

        @Test
        @DisplayName("최소 길이 경계값인 한 글자를 허용한다")
        void success_Of_MinLengthBoundary() {
            // given
            String value = "관";

            // when
            AdminRank rank = AdminRank.of(value);

            // then
            assertThat(rank.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("최대 길이 경계값인 50자를 허용한다")
        void success_Of_MaxLengthBoundary() {
            // given
            String value = "가".repeat(50);

            // when
            AdminRank rank = AdminRank.of(value);

            // then
            assertThat(rank.getValue()).hasSize(50);
        }

        @Test
        @DisplayName("null 직급은 거절한다")
        void fail_Of_Null_CustomException() {
            // given
            String value = null;

            // when & then
            assertThatThrownBy(() -> AdminRank.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("빈 직급은 거절한다")
        void fail_Of_Blank_CustomException() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> AdminRank.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("50자를 초과한 직급은 거절한다")
        void fail_Of_OverMaxLength_CustomException() {
            // given
            String value = "가".repeat(51);

            // when & then
            assertThatThrownBy(() -> AdminRank.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
