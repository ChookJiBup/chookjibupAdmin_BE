package com.example.chookjibupadmin.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 운영자 등록 요청 DTO이다.
 */
@Schema(description = "운영자 등록 요청")
public record RegisterOperatorRequest(
        @Schema(description = "운영자 이메일", example = "vendor@example.com")
        @Email
        @NotBlank
        String email,

        @Schema(description = "운영자 이름", example = "김운영")
        @NotBlank
        @Size(min = 2, max = 100)
        String name,

        @Schema(description = "업체명", example = "축제기획(주)")
        @NotBlank
        @Size(min = 2, max = 255)
        String companyName
) {
}
