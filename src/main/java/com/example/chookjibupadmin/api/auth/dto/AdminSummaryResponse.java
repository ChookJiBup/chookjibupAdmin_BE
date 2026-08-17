package com.example.chookjibupadmin.api.auth.dto;

import com.example.chookjibupadmin.admin.command.domain.AccountKind;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * 관리자 식별 정보와 역할별 권한 플래그를 제공하는 응답 DTO이다.
 */
@Schema(description = "관리자 요약 정보")
public record AdminSummaryResponse(
        @Schema(description = "외부 노출용 관리자 ID", example = "33333333-3333-3333-3333-333333333333")
        UUID adminId,

        @Schema(description = "외부 노출용 관리 대상 축제 ID. 축제 생성 전에는 null", example = "11111111-1111-1111-1111-111111111111")
        UUID festivalId,

        @Schema(description = "로그인 이메일", example = "admin@mapo.go.kr")
        String email,

        @Schema(description = "관리자 이름", example = "홍길동")
        String name,

        @Schema(description = "과·팀", example = "관광정책과")
        String organization,

        @Schema(description = "직급. 외부업자는 null", example = "과장")
        String rank,

        @Schema(description = "계정 종류", example = "GOVERNMENT")
        AccountKind accountKind,

        @Schema(description = "관리자 역할. 축제 생성 전에는 null", example = "FESTIVAL_OWNER")
        AdminRole role,

        @Schema(description = "서브관리자 초대 가능 여부", example = "true")
        boolean canInviteSubAdmin,

        @Schema(description = "행사 정보 수정 가능 여부", example = "true")
        boolean canModifyFestivalInfo,

        @Schema(description = "운영 보고서 조회 가능 여부", example = "true")
        boolean canViewOperationReport,

        @Schema(description = "줄 끝 갱신 가능 여부", example = "true")
        boolean canUpdateQueueTail
) {

    /**
     * 관리자 계정의 현재 역할과 권한 정보를 요약 응답으로 변환한다.
     */
    public static AdminSummaryResponse from(AdminAccount adminAccount) {
        return new AdminSummaryResponse(
                adminAccount.getPublicId(),
                null,
                adminAccount.getEmailValue(),
                adminAccount.getNameValue(),
                adminAccount.getOrganizationValue(),
                adminAccount.getRankValue(),
                adminAccount.getAccountKind(),
                null,
                false,
                false,
                false,
                false
        );
    }
}

