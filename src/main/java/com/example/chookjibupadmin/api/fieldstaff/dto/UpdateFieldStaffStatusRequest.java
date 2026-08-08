package com.example.chookjibupadmin.api.fieldstaff.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 현장 스태프 활성 상태 변경 요청 DTO이다.
 */
@Schema(description = "현장 스태프 활성 상태 변경 요청")
public record UpdateFieldStaffStatusRequest(
        @Schema(description = "활성화 여부", example = "false")
        @NotNull Boolean active
) {
}
