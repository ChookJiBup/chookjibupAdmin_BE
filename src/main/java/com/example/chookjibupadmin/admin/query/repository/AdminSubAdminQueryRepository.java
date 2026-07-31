package com.example.chookjibupadmin.admin.query.repository;

import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 축제별 서브관리자 조회 저장소 계약이다.
 */
public interface AdminSubAdminQueryRepository {

    /**
     * 지정한 축제에서 특정 제1 관리자가 초대한 활성 서브관리자 목록을 조회한다.
     */
    List<AdminSubAdminView> searchInvitedSubAdmins(
            Long festivalId,
            Long invitedByAdminId,
            String keyword
    );

    /**
     * 지정한 축제에서 특정 제1 관리자가 초대한 활성 서브관리자를 외부 UUID로 조회한다.
     */
    Optional<AdminSubAdminView> findInvitedSubAdmin(
            Long festivalId,
            Long invitedByAdminId,
            UUID publicId
    );
}
