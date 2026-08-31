package com.example.chookjibupadmin.operator.support;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** 현장 스태프 Access Token을 브라우저 스크립트에서 읽을 수 없는 쿠키로 관리한다. */
@Component
public class FieldStaffAuthCookieService {

    private final String cookieName;
    private final boolean secure;
    private final String sameSite;

    public FieldStaffAuthCookieService(
            @Value("${app.field-staff.jwt.cookie-name:chookjibup_staff_access}") String cookieName,
            @Value("${app.field-staff.jwt.cookie-secure:false}") boolean secure,
            @Value("${app.field-staff.jwt.cookie-same-site:Strict}") String sameSite
    ) {
        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public ResponseCookie create(String token, long expiresInSeconds) {
        return baseCookie(token).maxAge(Duration.ofSeconds(expiresInSeconds)).build();
    }

    public ResponseCookie expire() {
        return baseCookie("").maxAge(Duration.ZERO).build();
    }

    public String cookieName() {
        return cookieName;
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/");
    }
}
