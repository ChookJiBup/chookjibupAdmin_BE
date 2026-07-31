package com.example.chookjibupadmin.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 이메일 인증 코드 요청 DTO이다.
 */
@Schema(description = "관리자 이메일 인증 코드 요청")
public record AdminEmailVerificationRequest(
        @Schema(description = "정부 공식 이메일", example = "admin@mapo.go.kr")
        @Email
        @NotBlank
        String email
) {
}
