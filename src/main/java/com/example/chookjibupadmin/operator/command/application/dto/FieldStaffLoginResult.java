package com.example.chookjibupadmin.operator.command.application.dto;

import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import java.util.UUID;

/**
 * 현장 스태프 로그인 성공 결과이다.
 */
public record FieldStaffLoginResult(
        String accessToken,
        long expiresIn,
        FieldStaffAccount fieldStaffAccount,
        UUID festivalPublicId
) {
}
