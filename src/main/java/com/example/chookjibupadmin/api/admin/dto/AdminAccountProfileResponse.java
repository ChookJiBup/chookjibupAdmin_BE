package com.example.chookjibupadmin.api.admin.dto;

import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import com.example.chookjibupadmin.admin.query.application.dto.AdminAccountProfileView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * 관리자 본인 정보 조회 응답 DTO이다.
 */
@Schema(description = "관리자 본인 정보 조회 응답")
public record AdminAccountProfileResponse(
        @Schema(description = "외부 노출용 관리자 ID", example = "11111111-1111-1111-1111-111111111111")
        UUID adminId,

        @Schema(description = "로그인 이메일", example = "admin@mapo.go.kr")
        String email,

        @Schema(description = "관리자 이름", example = "홍길동")
        String name,

        @Schema(description = "과·팀", example = "관광정책과")
        String organization,

        @Schema(description = "직급", example = "과장")
        String rank,

        @Schema(description = "계정 상태", example = "ACTIVE")
        AdminStatus status
) {

    /**
     * 관리자 본인 정보 조회 결과를 HTTP 응답 DTO로 변환한다.
     */
    public static AdminAccountProfileResponse from(AdminAccountProfileView view) {
        return new AdminAccountProfileResponse(
                view.adminId(),
                view.email(),
                view.name(),
                view.organization(),
                view.rank(),
                view.status()
        );
    }
}
