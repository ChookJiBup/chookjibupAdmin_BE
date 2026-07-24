package com.example.demoadmin.api.auth.dto;

import com.example.demoadmin.admin.command.domain.AdminAccount;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 로그인 성공 시 발급된 토큰과 관리자 요약 정보를 반환한다.
 */
@Schema(description = "관리자 로그인 응답")
public record AdminLoginResponse(
        @Schema(description = "JWT Access Token")
        String accessToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token 만료 시간 초", example = "1800")
        long expiresIn,

        @Schema(description = "로그인 관리자 정보")
        AdminSummaryResponse admin
) {

    /**
     * Access Token과 관리자 계정 정보로 로그인 응답을 생성한다.
     */
    public static AdminLoginResponse of(
            String accessToken,
            long expiresIn,
            AdminAccount adminAccount
    ) {
        return new AdminLoginResponse(
                accessToken,
                "Bearer",
                expiresIn,
                AdminSummaryResponse.from(adminAccount)
        );
    }
}

