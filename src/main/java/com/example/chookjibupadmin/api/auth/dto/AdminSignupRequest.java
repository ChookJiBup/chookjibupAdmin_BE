package com.example.chookjibupadmin.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 관리자 계정 생성을 위한 HTTP 요청 DTO이다.
 */
@Schema(description = "관리자 회원가입 요청")
public record AdminSignupRequest(
        @Schema(description = "로그인 이메일", example = "admin@mapo.go.kr")
        @Email
        @NotBlank
        String email,

        @Schema(description = "관리자 이름", example = "홍길동")
        @NotBlank
        @Size(min = 2, max = 100)
        String name,

        @Schema(description = "소속 조직", example = "마포구청 소속")
        @NotBlank
        @Size(min = 2, max = 255)
        String organization,

        @Schema(description = "비밀번호", example = "Password!123")
        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @Schema(description = "비밀번호 확인", example = "Password!123")
        @NotBlank
        String passwordConfirm
) {
}

