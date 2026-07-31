package com.example.chookjibupadmin.api.fieldstaff.dto;

import com.example.chookjibupadmin.operator.command.application.dto.FieldStaffLoginResult;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * 현장 스태프 로그인 응답이다.
 */
@Schema(description = "현장 스태프 로그인 응답")
public record FieldStaffLoginResponse(
        @Schema(description = "JWT Access Token")
        String accessToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token 만료 시간 초", example = "1800")
        long expiresIn,

        @Schema(description = "현장 스태프 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID staffId,

        @Schema(description = "축제 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID festivalId,

        @Schema(description = "로그인 아이디", example = "staff01")
        String loginId,

        @Schema(description = "현장 스태프 사용자명", example = "김스태프")
        String name
) {

    /**
     * 현장 스태프 로그인 결과를 API 응답으로 변환한다.
     */
    public static FieldStaffLoginResponse from(FieldStaffLoginResult result) {
        FieldStaffAccount fieldStaffAccount = result.fieldStaffAccount();
        return new FieldStaffLoginResponse(
                result.accessToken(),
                "Bearer",
                result.expiresIn(),
                fieldStaffAccount.getPublicId(),
                result.festivalPublicId(),
                fieldStaffAccount.getLoginIdValue(),
                fieldStaffAccount.getNameValue()
        );
    }
}
