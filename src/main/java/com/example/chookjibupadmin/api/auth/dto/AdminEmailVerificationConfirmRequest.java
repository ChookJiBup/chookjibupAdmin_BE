package com.example.chookjibupadmin.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 관리자 이메일 인증 코드 확인 DTO이다.
 */
@Schema(description = "관리자 이메일 인증 코드 확인 요청")
public record AdminEmailVerificationConfirmRequest(
        @Schema(description = "정부 공식 이메일", example = "admin@mapo.go.kr")
        @Email
        @NotBlank
        String email,

        @Schema(description = "6자리 인증 코드", example = "123456")
        @NotBlank
        @Pattern(regexp = "^\\d{6}$")
        String code
) {
}
