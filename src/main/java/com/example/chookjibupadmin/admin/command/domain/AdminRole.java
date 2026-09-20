package com.example.chookjibupadmin.admin.command.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

/**
 * 축제 단위 관리자 하이라키와 역할별 기능 권한을 정의한다.
 *
 * <p>행사 진행자와 현장 운영자는 관리자 계정 역할이 아니라 운영 코드 기반
 * operator 도메인에서 별도로 처리한다.</p>
 *
 * <p><strong>선언 순서가 곧 역할의 세기다.</strong> 위에 선언된 역할이 더 높다.
 * {@link #highestOf(Collection)}가 이 순서에 기대므로, 역할을 추가할 때는 세기에 맞는
 * 자리에 넣어야 한다.</p>
 */
public enum AdminRole {
    FESTIVAL_OWNER(
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true
    ),
    SUB_ADMIN(
            false,
            false,
            true,
            true,
            true,
            true,
            false,
            false
    );

    private final boolean canInviteSubAdmin;
    private final boolean canModifyFestivalInfo;
    private final boolean canManageFieldStaff;
    private final boolean canManageQueueDesign;
    private final boolean canViewOperationReport;
    private final boolean canUpdateQueueTail;
    private final boolean canViewFestivalResultReport;
    private final boolean canEditFestivalMap;

    AdminRole(
            boolean canInviteSubAdmin,
            boolean canModifyFestivalInfo,
            boolean canManageFieldStaff,
            boolean canManageQueueDesign,
            boolean canViewOperationReport,
            boolean canUpdateQueueTail,
            boolean canViewFestivalResultReport,
            boolean canEditFestivalMap
    ) {
        this.canInviteSubAdmin = canInviteSubAdmin;
        this.canModifyFestivalInfo = canModifyFestivalInfo;
        this.canManageFieldStaff = canManageFieldStaff;
        this.canManageQueueDesign = canManageQueueDesign;
        this.canViewOperationReport = canViewOperationReport;
        this.canUpdateQueueTail = canUpdateQueueTail;
        this.canViewFestivalResultReport = canViewFestivalResultReport;
        this.canEditFestivalMap = canEditFestivalMap;
    }

    /**
     * 여러 축제에서 가진 역할 중 가장 높은 역할을 고른다. 배정이 없으면 null.
     *
     * <p><strong>이 값으로 권한을 판정하면 안 된다.</strong> 계정 전체를 훑어 만든 파생값이라
     * 「어느 축제에서든 한 번은 총괄이었다」는 뜻일 뿐, 지금 보고 있는 축제의 역할이 아니다.
     * 총괄 10곳·운영자 3곳인 계정이면 운영자인 축제에서도 FESTIVAL_OWNER가 나오므로,
     * 축제 안 화면은 반드시 그 축제의 {@link AdminFestivalRole}로 판정해야 한다.</p>
     *
     * <p>쓰임새는 축제를 고르기 전 화면(메인보드·축제등록·마이페이지)의 역할 뱃지 하나뿐이다.
     * 거기서는 판정할 축제 자체가 없어서 계정 단위 표시값이 필요하다.</p>
     */
    public static AdminRole highestOf(Collection<AdminRole> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparingInt(AdminRole::ordinal))
                .orElse(null);
    }

    /**
     * 서브관리자 초대 권한 여부를 반환한다.
     */
    public boolean canInviteSubAdmin() {
        return canInviteSubAdmin;
    }

    /**
     * 행사명, 기간, 장소 같은 행사 기본 정보 수정 권한 여부를 반환한다.
     */
    public boolean canModifyFestivalInfo() {
        return canModifyFestivalInfo;
    }

    /**
     * 현장 스태프 계정 추가와 삭제 권한 여부를 반환한다.
     */
    public boolean canManageFieldStaff() {
        return canManageFieldStaff;
    }

    /**
     * 대기열 설계와 부스 라인 관리 권한 여부를 반환한다.
     */
    public boolean canManageQueueDesign() {
        return canManageQueueDesign;
    }

    /**
     * 운영 리포트 조회 권한 여부를 반환한다.
     */
    public boolean canViewOperationReport() {
        return canViewOperationReport;
    }

    /**
     * 현장 줄 끝 라인 갱신 권한 여부를 반환한다.
     */
    public boolean canUpdateQueueTail() {
        return canUpdateQueueTail;
    }

    /**
     * 축제 결과 보고서 조회와 생성 권한 여부를 반환한다.
     *
     * <p>결과 보고서는 축제 전체 성과를 담고 있어 총괄관리자 전용이다.
     * 운영 중 화면인 대시보드({@link #canViewOperationReport()})와는 구분한다.</p>
     */
    public boolean canViewFestivalResultReport() {
        return canViewFestivalResultReport;
    }

    /**
     * 부스맵 편집기 조회와 부스 승인 권한 여부를 반환한다.
     *
     * <p>배치도 교체·저장·공개가 총괄관리자 전용이므로,
     * 같은 편집 흐름인 편집기 조회와 부스 승인도 총괄관리자 전용으로 둔다.</p>
     */
    public boolean canEditFestivalMap() {
        return canEditFestivalMap;
    }
}
