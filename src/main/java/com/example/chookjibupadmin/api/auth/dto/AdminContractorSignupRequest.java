package com.example.chookjibupadmin.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 외부업자 관리자 계정 생성을 위한 HTTP 요청 DTO이다.
 */
@Schema(description = "외부업자 관리자 회원가입 요청")
public record AdminContractorSignupRequest(
        @Schema(description = "외부업자 로그인 이메일", example = "vendor@gmail.com")
        @Email
        @NotBlank
        String email,

        @Schema(description = "이름", example = "김업체")
        @NotBlank
        @Size(min = 2, max = 100)
        String name,

        @Schema(description = "업체명", example = "축제기획(주)")
        @NotBlank
        @Size(min = 2, max = 255)
        String companyName,

        @Schema(description = "비밀번호", example = "Password!123")
        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @Schema(description = "비밀번호 확인", example = "Password!123")
        @NotBlank
        String passwordConfirm
) {
}
