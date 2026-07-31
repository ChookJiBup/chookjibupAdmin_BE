package com.example.chookjibupadmin.admin.command.application;

import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRoleRepository;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제별 관리자 역할 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminFestivalRoleService {

    private final AdminFestivalRoleRepository adminFestivalRoleRepository;

    @Transactional
    public AdminFestivalRole save(AdminFestivalRole adminFestivalRole) {
        return adminFestivalRoleRepository.save(adminFestivalRole);
    }

    @Transactional
    public AdminFestivalRole assignFestivalOwner(
            Long adminAccountId,
            Long festivalId
    ) {
        if (adminFestivalRoleRepository.existsByFestivalIdAndRole(
                festivalId,
                AdminRole.FESTIVAL_OWNER
        )) {
            throw new CustomException(ErrorCode.AUTH_FESTIVAL_OWNER_ALREADY_EXISTS);
        }
        if (adminFestivalRoleRepository.existsByAdminAccountIdAndFestivalId(
                adminAccountId,
                festivalId
        )) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_ALREADY_ASSIGNED);
        }

        return save(AdminFestivalRole.createFestivalOwner(
                adminAccountId,
                festivalId
        ));
    }

    @Transactional
    public AdminFestivalRole assignSubAdmin(
            Long adminAccountId,
            Long festivalId,
            Long invitedByAdminId
    ) {
        if (adminFestivalRoleRepository.existsByAdminAccountIdAndFestivalId(
                adminAccountId,
                festivalId
        )) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_ALREADY_ASSIGNED);
        }

        return save(AdminFestivalRole.createSubAdmin(
                adminAccountId,
                festivalId,
                invitedByAdminId
        ));
    }

    public AdminFestivalRole getByAdminAccountIdAndFestivalId(
            Long adminAccountId,
            Long festivalId
    ) {
        return adminFestivalRoleRepository
                .findByAdminAccountIdAndFestivalId(adminAccountId, festivalId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN));
    }
}
