package com.example.chookjibupadmin.operator.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FieldStaffLoginIdTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("로그인 아이디를 소문자로 정규화한다")
        void success_Of_Normalized() {
            // given
            String value = " Staff01 ";

            // when
            FieldStaffLoginId loginId = FieldStaffLoginId.of(value);

            // then
            assertThat(loginId.getValue()).isEqualTo("staff01");
        }

        @Test
        @DisplayName("최소 길이 로그인 아이디를 생성한다")
        void success_Of_MinBoundary() {
            // given
            String value = "abcd";

            // when
            FieldStaffLoginId loginId = FieldStaffLoginId.of(value);

            // then
            assertThat(loginId.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("최대 길이 로그인 아이디를 생성한다")
        void success_Of_MaxBoundary() {
            // given
            String value = "a".repeat(30);

            // when
            FieldStaffLoginId loginId = FieldStaffLoginId.of(value);

            // then
            assertThat(loginId.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("최소 길이보다 짧으면 생성할 수 없다")
        void fail_Of_Short_CustomException() {
            // given
            String value = "abc";

            // when & then
            assertThatThrownBy(() -> FieldStaffLoginId.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("최대 길이보다 길면 생성할 수 없다")
        void fail_Of_Long_CustomException() {
            // given
            String value = "a".repeat(31);

            // when & then
            assertThatThrownBy(() -> FieldStaffLoginId.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("허용되지 않는 문자가 있으면 생성할 수 없다")
        void fail_Of_InvalidCharacter_CustomException() {
            // given
            String value = "staff 01";

            // when & then
            assertThatThrownBy(() -> FieldStaffLoginId.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
