package com.example.chookjibupadmin.api.admin.dto;

import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * 서브관리자 조회 응답 DTO이다.
 */
@Schema(description = "서브관리자 조회 응답")
public record AdminSubAdminResponse(
        @Schema(description = "외부 노출용 관리자 ID", example = "33333333-3333-3333-3333-333333333333")
        UUID adminId,

        @Schema(description = "로그인 이메일", example = "sub@mapo.go.kr")
        String email,

        @Schema(description = "관리자 이름", example = "김관리")
        String name,

        @Schema(description = "소속 조직", example = "마포구청 소속")
        String organization,

        @Schema(description = "소속 부서", example = "관광정책과")
        String department,

        @Schema(description = "직급", example = "주무관")
        String rank,

        @Schema(description = "계정 상태", example = "ACTIVE")
        AdminStatus status
) {

    /**
     * 서브관리자 조회 결과를 HTTP 응답 DTO로 변환한다.
     */
    public static AdminSubAdminResponse from(AdminSubAdminView view) {
        return new AdminSubAdminResponse(
                view.adminId(),
                view.email(),
                view.name(),
                view.organization(),
                view.department(),
                view.rank(),
                view.status()
        );
    }
}
