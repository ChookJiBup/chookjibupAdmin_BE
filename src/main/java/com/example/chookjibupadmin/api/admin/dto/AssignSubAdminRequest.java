package com.example.chookjibupadmin.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 제2관리자로 배정할 관리자 계정 요청 DTO이다.
 */
@Schema(description = "제2관리자 권한 부여 요청")
public record AssignSubAdminRequest(
        @Schema(description = "배정할 관리자 계정 UUID")
        @NotNull UUID adminId
) {
}
