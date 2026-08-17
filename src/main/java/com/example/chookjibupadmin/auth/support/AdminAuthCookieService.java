package com.example.chookjibupadmin.auth.support;

import com.example.chookjibupadmin.auth.command.infrastructure.JwtProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** 관리자 Access Token을 브라우저 스크립트에서 읽을 수 없는 쿠키로 관리한다. */
@Component
@RequiredArgsConstructor
public class AdminAuthCookieService {

    private final JwtProperties jwtProperties;

    public ResponseCookie create(String accessToken, long expiresInSeconds) {
        return baseCookie(accessToken)
                .maxAge(Duration.ofSeconds(expiresInSeconds))
                .build();
    }

    public ResponseCookie expire() {
        return baseCookie("").maxAge(Duration.ZERO).build();
    }

    public String cookieName() {
        return jwtProperties.cookieName();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(jwtProperties.cookieName(), value)
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .sameSite(jwtProperties.cookieSameSite())
                .path("/");
    }
}
