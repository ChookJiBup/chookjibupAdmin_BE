package com.example.chookjibupadmin.admin.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdminEmailTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("JPA 기본 생성자로 생성할 수 있다")
        void success_Constructor_ForJpa() {
            // given

            // when
            AdminEmail email = new AdminEmail();

            // then
            assertThat(email.getValue()).isNull();
        }
    }

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("관리자 이메일은 앞뒤 공백을 제거하고 소문자로 변환한다")
        void success_Of_Normalized() {
            // given
            String value = " Admin@MAPO.GO.KR ";

            // when
            AdminEmail email = AdminEmail.of(value);

            // then
            assertThat(email.getValue()).isEqualTo("admin@mapo.go.kr");
        }

        @Test
        @DisplayName("공직자 통합메일 도메인이면 생성한다")
        void success_Of_KoreaDomain() {
            // given
            String value = "admin@korea.kr";

            // when
            AdminEmail email = AdminEmail.of(value);

            // then
            assertThat(email.getValue()).isEqualTo("admin@korea.kr");
        }

        @Test
        @DisplayName("정부 도메인 최소 경계값이면 생성한다")
        void success_Of_GoKrBoundary() {
            // given
            String value = "a@go.kr";

            // when
            AdminEmail email = AdminEmail.of(value);

            // then
            assertThat(email.getValue()).isEqualTo("a@go.kr");
        }

        @Test
        @DisplayName("로컬 테스트용 네이버 도메인이면 임시로 생성한다")
        void success_Of_TemporaryNaverDomain() {
            // given
            String value = "dlgkrwns213@naver.com";

            // when
            AdminEmail email = AdminEmail.of(value);

            // then
            assertThat(email.getValue()).isEqualTo("dlgkrwns213@naver.com");
        }

        @Test
        @DisplayName("관리자 이메일이 null이면 생성할 수 없다")
        void fail_Of_Null_CustomException() {
            // given
            String value = null;

            // when & then
            assertThatThrownBy(() -> AdminEmail.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("관리자 이메일이 빈 값이면 생성할 수 없다")
        void fail_Of_Blank_CustomException() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> AdminEmail.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("관리자 이메일 형식이 올바르지 않으면 생성할 수 없다")
        void fail_Of_InvalidFormat_CustomException() {
            // given
            String value = "invalid-email";

            // when & then
            assertThatThrownBy(() -> AdminEmail.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("정부 공식 이메일 도메인이 아니면 생성할 수 없다")
        void fail_Of_NotGovernmentDomain_CustomException() {
            // given
            String value = "admin@example.com";

            // when & then
            assertThatThrownBy(() -> AdminEmail.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_EMAIL_DOMAIN_NOT_ALLOWED.getMessage());
        }
    }
}
