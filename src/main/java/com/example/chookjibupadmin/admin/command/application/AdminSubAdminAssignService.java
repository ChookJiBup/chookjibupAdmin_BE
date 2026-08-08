package com.example.chookjibupadmin.admin.command.application;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 제1관리자가 활성 관리자 계정을 담당 축제의 제2관리자로 배정한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminSubAdminAssignService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService roleService;
    private final FestivalService festivalService;

    /**
     * 요청 관리자의 제1관리자 권한을 확인하고 대상 계정에 축제 역할을 부여한다.
     */
    public AdminFestivalRole assign(
            UUID festivalId,
            UUID targetAdminId,
            AdminPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        AdminAccount owner = adminAccountService.getById(principal.adminId());
        if (!owner.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }
        Festival festival = festivalService.getByPublicId(festivalId);
        AdminFestivalRole ownerRole = roleService
                .getByAdminAccountIdAndFestivalId(owner.getId(), festival.getId());
        if (!ownerRole.canInviteSubAdmin()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        AdminAccount target = adminAccountService.findByPublicId(targetAdminId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.ADMIN_SUB_ADMIN_NOT_FOUND
                ));
        if (!target.isActive() || target.getId().equals(owner.getId())) {
            throw new CustomException(ErrorCode.ADMIN_SUB_ADMIN_NOT_FOUND);
        }
        return roleService.assignSubAdmin(
                target.getId(),
                festival.getId(),
                owner.getId()
        );
    }
}
