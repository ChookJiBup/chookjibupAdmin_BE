package com.example.chookjibupadmin.admin.query.application;

import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminView;
import com.example.chookjibupadmin.admin.query.repository.AdminSubAdminQueryRepository;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서브관리자 조회 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSubAdminQueryService {

    private final AdminSubAdminQueryRepository queryRepository;

    /**
     * 지정한 축제에서 특정 제1 관리자가 초대한 활성 서브관리자 목록을 조회한다.
     */
    public List<AdminSubAdminView> findInvitedSubAdmins(
            Long festivalId,
            Long invitedByAdminId
    ) {
        return queryRepository.findInvitedSubAdmins(
                festivalId,
                invitedByAdminId
        );
    }

    /**
     * 지정한 축제에서 특정 제1 관리자가 초대한 활성 서브관리자를 외부 UUID로 조회한다.
     */
    public AdminSubAdminView getInvitedSubAdmin(
            Long festivalId,
            Long invitedByAdminId,
            UUID publicId
    ) {
        return queryRepository.findInvitedSubAdmin(
                        festivalId,
                        invitedByAdminId,
                        publicId
                )
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_SUB_ADMIN_NOT_FOUND));
    }
}
