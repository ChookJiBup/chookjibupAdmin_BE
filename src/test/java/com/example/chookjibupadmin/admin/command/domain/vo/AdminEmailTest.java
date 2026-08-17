package com.example.chookjibupadmin.admin.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.domain.AccountKind;
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
        @DisplayName("일반 이메일 형식이면 도메인 제한 없이 생성한다")
        void success_Of_GeneralEmail() {
            // given
            String value = "vendor@example.com";

            // when
            AdminEmail email = AdminEmail.of(value);

            // then
            assertThat(email.getValue()).isEqualTo("vendor@example.com");
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
        @DisplayName("관리자 이메일 형식이 올바르지 않으면 생성할 수 없다")
        void fail_Of_InvalidFormat_CustomException() {
            // given
            String value = "invalid-email";

            // when & then
            assertThatThrownBy(() -> AdminEmail.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }

    @Nested
    @DisplayName("isGovernmentDomain")
    class IsGovernmentDomain {

        @Test
        @DisplayName("go.kr 도메인이면 true이다")
        void success_IsGovernmentDomain_GoKr() {
            // given
            AdminEmail email = AdminEmail.of("admin@mapo.go.kr");

            // when & then
            assertThat(email.isGovernmentDomain()).isTrue();
        }

        @Test
        @DisplayName("korea.kr 도메인이면 true이다")
        void success_IsGovernmentDomain_KoreaKr() {
            // given
            AdminEmail email = AdminEmail.of("admin@korea.kr");

            // when & then
            assertThat(email.isGovernmentDomain()).isTrue();
        }

        @Test
        @DisplayName("일반 이메일이면 false이다")
        void success_IsGovernmentDomain_GeneralEmail() {
            // given
            AdminEmail email = AdminEmail.of("vendor@gmail.com");

            // when & then
            assertThat(email.isGovernmentDomain()).isFalse();
        }
    }

    @Nested
    @DisplayName("of with AccountKind")
    class OfWithAccountKind {

        @Test
        @DisplayName("공무원 가입은 정부 이메일만 허용한다")
        void success_Of_GovernmentAccountKind() {
            // given
            String value = "admin@mapo.go.kr";

            // when
            AdminEmail email = AdminEmail.of(value, AccountKind.GOVERNMENT);

            // then
            assertThat(email.getValue()).isEqualTo("admin@mapo.go.kr");
        }

        @Test
        @DisplayName("공무원 가입에 일반 이메일이면 거절한다")
        void fail_Of_GovernmentAccountKind_GeneralEmail() {
            // given
            String value = "vendor@gmail.com";

            // when & then
            assertThatThrownBy(() -> AdminEmail.of(value, AccountKind.GOVERNMENT))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_GOVERNMENT_EMAIL_REQUIRED.getMessage());
        }

        @Test
        @DisplayName("외부업자 가입은 일반 이메일을 허용한다")
        void success_Of_ContractorAccountKind() {
            // given
            String value = "vendor@gmail.com";

            // when
            AdminEmail email = AdminEmail.of(value, AccountKind.CONTRACTOR);

            // then
            assertThat(email.getValue()).isEqualTo("vendor@gmail.com");
        }

        @Test
        @DisplayName("외부업자 가입에 정부 이메일이면 거절한다")
        void fail_Of_ContractorAccountKind_GovernmentEmail() {
            // given
            String value = "admin@mapo.go.kr";

            // when & then
            assertThatThrownBy(() -> AdminEmail.of(value, AccountKind.CONTRACTOR))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            ErrorCode.AUTH_CONTRACTOR_GOVERNMENT_EMAIL_NOT_ALLOWED.getMessage()
                    );
        }
    }
}
