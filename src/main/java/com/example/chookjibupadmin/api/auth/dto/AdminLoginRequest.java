package com.example.chookjibupadmin.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 로그인을 위한 HTTP 요청 DTO이다.
 */
@Schema(description = "관리자 로그인 요청")
public record AdminLoginRequest(
        @Schema(description = "로그인 이메일", example = "admin@mapo.go.kr")
        @Email
        @NotBlank
        String email,

        @Schema(description = "비밀번호", example = "Password!123")
        @NotBlank
        String password
) {
}

