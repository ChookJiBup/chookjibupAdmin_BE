package com.example.chookjibupadmin.auth.support;

/**
 * JWT 인증 후 SecurityContext에 저장되는 관리자 인증 주체이다.
 */
public record AdminPrincipal(
        Long adminId,
        String email,
        long authVersion
) implements FestivalActorPrincipal {

    public AdminPrincipal(Long adminId, String email) {
        this(adminId, email, 0L);
    }
}
