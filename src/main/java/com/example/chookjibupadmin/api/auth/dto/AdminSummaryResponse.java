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

        @Schema(description = "계정 종류. CONTRACTOR는 축제 배정 시 제2관리자와 동일 권한·축제 생성 불가", example = "GOVERNMENT")
        AccountKind accountKind,

        @Schema(
                description = "계정이 가진 축제 역할 중 가장 높은 역할. 배정된 축제가 없으면 null. "
                        + "축제를 고르기 전 화면의 역할 뱃지 표시용이며 권한 판정에 쓰면 안 된다.",
                example = "FESTIVAL_OWNER"
        )
        AdminRole role,

        @Schema(description = "서브관리자 초대 가능 여부. 축제를 고르기 전에는 항상 false", example = "false")
        boolean canInviteSubAdmin,

        @Schema(description = "행사 정보 수정 가능 여부. 축제를 고르기 전에는 항상 false", example = "false")
        boolean canModifyFestivalInfo,

        @Schema(description = "운영 보고서 조회 가능 여부. 축제를 고르기 전에는 항상 false", example = "false")
        boolean canViewOperationReport,

        @Schema(description = "줄 끝 갱신 가능 여부. 축제를 고르기 전에는 항상 false", example = "false")
        boolean canUpdateQueueTail
) {

    /**
     * 관리자 계정과 계정 단위 대표 역할을 요약 응답으로 변환한다.
     *
     * <p>{@code highestRole}은 이 계정이 어느 축제에서든 가진 역할 중 가장 높은 것으로,
     * 축제를 고르기 전 화면(메인보드·축제등록·마이페이지)의 역할 뱃지를 그리기 위한
     * <strong>표시값</strong>이다. 그 화면에는 판정할 축제 자체가 없다.</p>
     *
     * <p>권한 플래그는 채우지 않고 false로 둔다. 이 값들은 모두 특정 축제의
     * {@code AdminFestivalRole}에서만 의미가 있는데, 총괄 10곳·운영자 3곳인 계정에
     * 계정 단위로 canInviteSubAdmin=true를 내려주면 운영자로 배정된 축제에서도 총괄
     * 메뉴가 열려 버린다. 축제 안 화면은 그 축제의 역할을 따로 받아 판정해야 한다.</p>
     */
    public static AdminSummaryResponse from(
            AdminAccount adminAccount,
            AdminRole highestRole
    ) {
        return new AdminSummaryResponse(
                adminAccount.getPublicId(),
                null,
                adminAccount.getEmailValue(),
                adminAccount.getNameValue(),
                adminAccount.getOrganizationValue(),
                adminAccount.getRankValue(),
                adminAccount.getAccountKind(),
                highestRole,
                false,
                false,
                false,
                false
        );
    }
}

