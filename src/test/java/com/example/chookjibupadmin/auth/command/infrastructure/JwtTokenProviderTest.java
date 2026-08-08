package com.example.chookjibupadmin.auth.command.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            new JwtProperties("test-secret-key", 1800)
    );

    @Nested
    @DisplayName("createAccessToken")
    class CreateAccessToken {

        @Test
        @DisplayName("관리자 토큰은 계정 식별자와 이메일을 포함한다")
        void success_CreateAccessToken() {
            // given
            AdminAccount adminAccount = adminAccount();
            ReflectionTestUtils.setField(adminAccount, "id", 1L);

            // when
            String accessToken = jwtTokenProvider.createAccessToken(adminAccount);
            AdminPrincipal principal = jwtTokenProvider.parse(accessToken);

            // then
            assertThat(principal.adminId()).isEqualTo(1L);
            assertThat(principal.email()).isEqualTo("owner@mapo.go.kr");
            assertThat(principal.authVersion()).isZero();
        }

        @Test
        @DisplayName("서명이 변조된 관리자 토큰은 거부한다")
        void fail_Parse_InvalidSignature_CustomException() {
            // given
            AdminAccount adminAccount = adminAccount();
            ReflectionTestUtils.setField(adminAccount, "id", 1L);
            String token = jwtTokenProvider.createAccessToken(adminAccount);
            char replacement = token.charAt(token.length() - 1) == 'A' ? 'B' : 'A';
            String tamperedToken = token.substring(0, token.length() - 1) + replacement;

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.parse(tamperedToken))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        }
    }

    private AdminAccount adminAccount() {
        return AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                AdminPasswordHash.of("encoded-password")
        );
    }
}
