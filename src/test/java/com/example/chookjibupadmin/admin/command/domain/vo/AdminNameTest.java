package com.example.chookjibupadmin.admin.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminNameTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("JPA 기본 생성자로 생성할 수 있다")
        void success_Constructor_ForJpa() {
            // given

            // when
            AdminName name = new AdminName();

            // then
            assertThat(name.getValue()).isNull();
        }
    }

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("관리자 이름은 앞뒤 공백을 제거한다")
        void success_Of_Normalized() {
            // given
            String value = " 홍길동 ";

            // when
            AdminName name = AdminName.of(value);

            // then
            assertThat(name.getValue()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("관리자 이름은 최소 길이 경계값이면 생성한다")
        void success_Of_MinLengthBoundary() {
            // given
            String value = "홍길";

            // when
            AdminName name = AdminName.of(value);

            // then
            assertThat(name.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("관리자 이름은 최대 길이 경계값이면 생성한다")
        void success_Of_MaxLengthBoundary() {
            // given
            String value = "가".repeat(100);

            // when
            AdminName name = AdminName.of(value);

            // then
            assertThat(name.getValue()).hasSize(100);
        }

        @Test
        @DisplayName("관리자 이름이 null이면 생성할 수 없다")
        void fail_Of_Null_CustomException() {
            // given
            String value = null;

            // when & then
            assertThatThrownBy(() -> AdminName.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("관리자 이름은 최소 길이보다 짧으면 생성할 수 없다")
        void fail_Of_UnderMinLength_CustomException() {
            // given
            String value = "김";

            // when & then
            assertThatThrownBy(() -> AdminName.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("관리자 이름은 최대 길이보다 길면 생성할 수 없다")
        void fail_Of_OverMaxLength_CustomException() {
            // given
            String value = "가".repeat(101);

            // when & then
            assertThatThrownBy(() -> AdminName.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
