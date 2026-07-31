package com.example.chookjibupadmin.api.fieldstaff.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 여러 현장 스태프 계정의 일괄 삭제 요청이다.
 */
@Schema(description = "현장 스태프 계정 일괄 삭제 요청")
public record DeleteFieldStaffRequest(
        @Schema(description = "삭제할 현장 스태프 UUID 목록")
        @NotEmpty
        @Size(max = 100)
        List<@NotNull UUID> staffIds
) {
}
