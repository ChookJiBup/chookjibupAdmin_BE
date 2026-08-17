package com.example.chookjibupadmin.api.auth.dto;

import com.example.chookjibupadmin.admin.command.domain.AccountKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 관리자 이메일 인증 코드 요청 DTO이다.
 */
@Schema(description = "관리자 이메일 인증 코드 요청")
public record AdminEmailVerificationRequest(
        @Schema(description = "가입 이메일", example = "admin@mapo.go.kr")
        @Email
        @NotBlank
        String email,

        @Schema(description = "가입 계정 종류", example = "GOVERNMENT")
        @NotNull
        AccountKind accountKind
) {
}
