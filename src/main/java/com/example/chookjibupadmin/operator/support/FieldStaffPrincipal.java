package com.example.chookjibupadmin.operator.support;

import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;

/**
 * JWT 인증 후 SecurityContext에 저장되는 현장 스태프 인증 주체이다.
 */
public record FieldStaffPrincipal(
        Long fieldStaffId,
        Long festivalId,
        String loginId,
        long authVersion
) implements FestivalActorPrincipal {
}
