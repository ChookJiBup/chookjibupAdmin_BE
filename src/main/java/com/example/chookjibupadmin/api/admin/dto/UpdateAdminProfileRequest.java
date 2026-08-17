package com.example.chookjibupadmin.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 관리자 본인 프로필 수정 요청이다. */
@Schema(description = "관리자 본인 프로필 수정 요청")
public record UpdateAdminProfileRequest(
        @NotBlank @Size(min = 2, max = 100)
        @Schema(description = "이름", example = "홍길동")
        String name,

        @NotBlank @Size(min = 2, max = 100)
        @Schema(description = "부서", example = "문화예술과")
        String department,

        @NotBlank @Size(max = 50)
        @Schema(description = "직급", example = "주무관")
        String rank
) {
}
