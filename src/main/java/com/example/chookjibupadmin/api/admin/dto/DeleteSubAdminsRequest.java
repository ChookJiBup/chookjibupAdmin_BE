package com.example.chookjibupadmin.api.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 여러 제2관리자의 축제 권한 일괄 삭제 요청이다.
 */
@Schema(description = "제2관리자 일괄 삭제 요청")
public record DeleteSubAdminsRequest(
        @Schema(description = "삭제할 제2관리자 UUID 목록")
        @NotEmpty
        @Size(max = 100)
        List<@NotNull UUID> adminIds
) {
}
