package com.example.chookjibupadmin.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 재설정 토큰과 새 비밀번호를 전달하는 요청 DTO이다.
 */
@Schema(description = "관리자 비밀번호 재설정 확정 요청")
public record AdminPasswordResetConfirmRequest(
        @Schema(description = "이메일 링크에 포함된 일회용 토큰")
        @NotBlank
        @Size(min = 40, max = 100)
        String token,

        @Schema(description = "새 비밀번호", example = "NewPassword!123")
        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @Schema(description = "새 비밀번호 확인", example = "NewPassword!123")
        @NotBlank
        @Size(min = 8, max = 100)
        String passwordConfirm
) {
}
