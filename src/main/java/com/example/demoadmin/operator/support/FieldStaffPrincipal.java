package com.example.demoadmin.operator.support;

import com.example.demoadmin.auth.support.FestivalAccessPrincipal;

/**
 * JWT 인증 후 SecurityContext에 저장되는 현장 스태프 인증 주체이다.
 */
public record FieldStaffPrincipal(
        Long fieldStaffId,
        Long festivalId,
        String loginId
) implements FestivalAccessPrincipal {
}
