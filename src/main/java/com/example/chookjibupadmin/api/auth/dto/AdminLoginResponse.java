package com.example.chookjibupadmin.api.auth.dto;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 로그인 성공 시 발급된 토큰과 관리자 요약 정보를 반환한다.
 */
@Schema(description = "관리자 로그인 응답")
public record AdminLoginResponse(
        @JsonIgnore
        @Schema(hidden = true)
        String accessToken,

        @Schema(description = "Access Token 만료 시간 초", example = "28800")
        long expiresIn,

        @Schema(description = "로그인 관리자 정보")
        AdminSummaryResponse admin
) {

    /**
     * Access Token과 관리자 계정 정보로 로그인 응답을 생성한다.
     *
     * <p>{@code highestRole}은 로그인 직후 역할 뱃지를 그리기 위한 계정 단위 표시값이다.
     * 권한 판정용이 아니다 — {@link AdminSummaryResponse#from} 참고.</p>
     */
    public static AdminLoginResponse of(
            String accessToken,
            long expiresIn,
            AdminAccount adminAccount,
            AdminRole highestRole
    ) {
        return new AdminLoginResponse(
                accessToken,
                expiresIn,
                AdminSummaryResponse.from(adminAccount, highestRole)
        );
    }
}
