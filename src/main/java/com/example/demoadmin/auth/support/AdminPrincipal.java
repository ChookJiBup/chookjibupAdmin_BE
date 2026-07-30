package com.example.demoadmin.auth.support;

import com.example.demoadmin.admin.command.domain.AdminRole;

/**
 * JWT 인증 후 SecurityContext에 저장되는 관리자 인증 주체이다.
 */
public record AdminPrincipal(
        Long adminId,
        String email
) implements FestivalAccessPrincipal {

    /**
     * TODO(auth): 기존 테스트 Fixture 정리 완료 후 제거한다.
     */
    @Deprecated(forRemoval = true)
    public AdminPrincipal(
            Long adminId,
            Long ignoredFestivalId,
            String email,
            AdminRole ignoredRole
    ) {
        this(adminId, email);
    }
}
