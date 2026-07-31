package com.example.chookjibupadmin.admin.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminPasswordHashTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("JPA 기본 생성자로 생성할 수 있다")
        void success_Constructor_ForJpa() {
            // given

            // when
            AdminPasswordHash passwordHash = new AdminPasswordHash();

            // then
            assertThat(passwordHash.getValue()).isNull();
        }
    }

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("비밀번호 해시를 생성한다")
        void success_Of() {
            // given
            String value = "{bcrypt}$2a$10$hashed-password";

            // when
            AdminPasswordHash passwordHash = AdminPasswordHash.of(value);

            // then
            assertThat(passwordHash.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("비밀번호 해시는 최대 길이 경계값이면 생성한다")
        void success_Of_MaxLengthBoundary() {
            // given
            String value = "a".repeat(255);

            // when
            AdminPasswordHash passwordHash = AdminPasswordHash.of(value);

            // then
            assertThat(passwordHash.getValue()).hasSize(255);
        }

        @Test
        @DisplayName("비밀번호 해시가 null이면 생성할 수 없다")
        void fail_Of_Null_CustomException() {
            // given
            String value = null;

            // when & then
            assertThatThrownBy(() -> AdminPasswordHash.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("비밀번호 해시가 공백이면 생성할 수 없다")
        void fail_Of_Blank_CustomException() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> AdminPasswordHash.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("비밀번호 해시는 최대 길이보다 길면 생성할 수 없다")
        void fail_Of_OverMaxLength_CustomException() {
            // given
            String value = "a".repeat(256);

            // when & then
            assertThatThrownBy(() -> AdminPasswordHash.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
