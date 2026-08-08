package com.example.chookjibupadmin.api.fieldstaff.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 재발급된 현장 스태프 임시 비밀번호 응답 DTO이다.
 */
@Schema(description = "현장 스태프 임시 비밀번호 재발급 응답")
public record ReissueFieldStaffPasswordResponse(
        @Schema(description = "재발급된 임시 비밀번호")
        String temporaryPassword
) {
}
