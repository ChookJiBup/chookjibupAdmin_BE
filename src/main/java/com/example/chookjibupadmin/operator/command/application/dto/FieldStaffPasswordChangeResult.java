package com.example.chookjibupadmin.operator.command.application.dto;

/**
 * 비밀번호 변경으로 기존 토큰이 무효해지므로 함께 재발급한 Access Token 결과이다.
 */
public record FieldStaffPasswordChangeResult(
        String accessToken,
        long expiresIn
) {
}
