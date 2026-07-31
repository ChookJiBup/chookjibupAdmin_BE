package com.example.chookjibupadmin.operator.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FieldStaffPasswordHashTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("비밀번호 해시를 생성한다")
        void success_Of() {
            // given
            String value = "encoded-password";

            // when
            FieldStaffPasswordHash passwordHash = FieldStaffPasswordHash.of(value);

            // then
            assertThat(passwordHash.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("최대 길이 해시를 생성한다")
        void success_Of_MaxBoundary() {
            // given
            String value = "a".repeat(255);

            // when
            FieldStaffPasswordHash passwordHash = FieldStaffPasswordHash.of(value);

            // then
            assertThat(passwordHash.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("빈 비밀번호 해시는 생성할 수 없다")
        void fail_Of_Blank_CustomException() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> FieldStaffPasswordHash.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("최대 길이보다 긴 해시는 생성할 수 없다")
        void fail_Of_Long_CustomException() {
            // given
            String value = "a".repeat(256);

            // when & then
            assertThatThrownBy(() -> FieldStaffPasswordHash.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
