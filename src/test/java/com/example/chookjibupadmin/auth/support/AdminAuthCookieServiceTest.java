package com.example.chookjibupadmin.auth.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.auth.command.infrastructure.JwtProperties;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class AdminAuthCookieServiceTest {

    private static final long EIGHT_HOURS_SECONDS = 28800L;

    private final AdminAuthCookieService cookieService = new AdminAuthCookieService(new JwtProperties(
            "test-jwt-secret-key-for-chookjibup-admin-local-tests",
            EIGHT_HOURS_SECONDS,
            "chookjibup_admin_access",
            true,
            "Strict"
    ));

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("쿠키 Max-Age는 전달받은 토큰 만료 시간을 그대로 따른다")
        void success_MaxAgeFollowsTokenExpiration() {
            // when
            ResponseCookie cookie = cookieService.create("access-token", EIGHT_HOURS_SECONDS);

            // then
            assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(EIGHT_HOURS_SECONDS));
            assertThat(cookie.getName()).isEqualTo("chookjibup_admin_access");
            assertThat(cookie.getValue()).isEqualTo("access-token");
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.isSecure()).isTrue();
            assertThat(cookie.getSameSite()).isEqualTo("Strict");
            assertThat(cookie.getPath()).isEqualTo("/");
        }
    }

    @Nested
    @DisplayName("expire")
    class Expire {

        @Test
        @DisplayName("만료 쿠키는 값이 비고 Max-Age가 0이다")
        void success_ExpiredCookie() {
            // when
            ResponseCookie cookie = cookieService.expire();

            // then
            assertThat(cookie.getValue()).isEmpty();
            assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
        }
    }
}
