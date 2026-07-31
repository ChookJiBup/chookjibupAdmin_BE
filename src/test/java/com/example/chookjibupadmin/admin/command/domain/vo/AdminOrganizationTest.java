package com.example.chookjibupadmin.admin.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminOrganizationTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("JPA 기본 생성자로 생성할 수 있다")
        void success_Constructor_ForJpa() {
            // given

            // when
            AdminOrganization organization = new AdminOrganization();

            // then
            assertThat(organization.getValue()).isNull();
        }
    }

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("관리자 조직은 앞뒤 공백을 제거한다")
        void success_Of_Normalized() {
            // given
            String value = " 서울시 소속 ";

            // when
            AdminOrganization organization = AdminOrganization.of(value);

            // then
            assertThat(organization.getValue()).isEqualTo("서울시 소속");
        }

        @Test
        @DisplayName("관리자 조직은 최소 길이 경계값이면 생성한다")
        void success_Of_MinLengthBoundary() {
            // given
            String value = "서울";

            // when
            AdminOrganization organization = AdminOrganization.of(value);

            // then
            assertThat(organization.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("관리자 조직은 최대 길이 경계값이면 생성한다")
        void success_Of_MaxLengthBoundary() {
            // given
            String value = "가".repeat(255);

            // when
            AdminOrganization organization = AdminOrganization.of(value);

            // then
            assertThat(organization.getValue()).hasSize(255);
        }

        @Test
        @DisplayName("관리자 조직이 null이면 생성할 수 없다")
        void fail_Of_Null_CustomException() {
            // given
            String value = null;

            // when & then
            assertThatThrownBy(() -> AdminOrganization.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("관리자 조직은 빈 값이면 생성할 수 없다")
        void fail_Of_Blank_CustomException() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> AdminOrganization.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("관리자 조직은 최대 길이보다 길면 생성할 수 없다")
        void fail_Of_OverMaxLength_CustomException() {
            // given
            String value = "가".repeat(256);

            // when & then
            assertThatThrownBy(() -> AdminOrganization.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
