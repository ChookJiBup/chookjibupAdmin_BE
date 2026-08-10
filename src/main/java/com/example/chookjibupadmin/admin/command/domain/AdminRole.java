package com.example.chookjibupadmin.admin.command.domain;

/**
 * 축제 단위 관리자 하이라키와 역할별 기능 권한을 정의한다.
 *
 * <p>행사 진행자와 현장 운영자는 관리자 계정 역할이 아니라 운영 코드 기반
 * operator 도메인에서 별도로 처리한다.</p>
 */
public enum AdminRole {
    FESTIVAL_OWNER(
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
            true
    );

    private final boolean canInviteSubAdmin;
    private final boolean canModifyFestivalInfo;
    private final boolean canManageFieldStaff;
    private final boolean canManageQueueDesign;
    private final boolean canViewOperationReport;
    private final boolean canUpdateQueueTail;

    AdminRole(
            boolean canInviteSubAdmin,
            boolean canModifyFestivalInfo,
            boolean canManageFieldStaff,
            boolean canManageQueueDesign,
            boolean canViewOperationReport,
            boolean canUpdateQueueTail
    ) {
        this.canInviteSubAdmin = canInviteSubAdmin;
        this.canModifyFestivalInfo = canModifyFestivalInfo;
        this.canManageFieldStaff = canManageFieldStaff;
        this.canManageQueueDesign = canManageQueueDesign;
        this.canViewOperationReport = canViewOperationReport;
        this.canUpdateQueueTail = canUpdateQueueTail;
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
}
