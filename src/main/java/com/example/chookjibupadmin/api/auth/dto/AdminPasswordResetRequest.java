package com.example.chookjibupadmin.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 비밀번호 재설정 링크 요청 DTO이다.
 */
@Schema(description = "관리자 비밀번호 재설정 링크 요청")
public record AdminPasswordResetRequest(
        @Schema(description = "가입한 관리자 이메일", example = "admin@mapo.go.kr")
        @Email
        @NotBlank
        String email
) {
}
