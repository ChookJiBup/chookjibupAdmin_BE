package com.example.chookjibupadmin.api.fieldstaff.dto;

import com.example.chookjibupadmin.operator.command.application.dto.FieldStaffPasswordChangeResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 현장 스태프 비밀번호 변경 응답이다.
 *
 * <p>비밀번호를 바꾸면 기존 Access Token이 무효해지므로 새 토큰을 함께 내려준다.
 * 쿠키로도 갱신되므로 쿠키 인증만 쓰는 화면은 이 값을 쓰지 않아도 된다.</p>
 */
@Schema(description = "현장 스태프 비밀번호 변경 응답")
public record FieldStaffPasswordChangeResponse(
        @Schema(description = "새로 발급한 JWT Access Token")
        String accessToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token 만료 시간 초", example = "28800")
        long expiresIn,

        @Schema(description = "비밀번호 변경이 더 필요한지 여부", example = "false")
        boolean passwordChangeRequired
) {

    /**
     * 비밀번호 변경 결과를 API 응답으로 변환한다.
     */
    public static FieldStaffPasswordChangeResponse from(
            FieldStaffPasswordChangeResult result
    ) {
        return new FieldStaffPasswordChangeResponse(
                result.accessToken(),
                "Bearer",
                result.expiresIn(),
                false
        );
    }
}
