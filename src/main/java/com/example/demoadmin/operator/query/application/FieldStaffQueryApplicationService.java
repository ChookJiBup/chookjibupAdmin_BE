package com.example.demoadmin.operator.query.application;

import com.example.demoadmin.admin.command.application.AdminAccountService;
import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.admin.command.domain.AdminAccount;
import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.festival.command.application.FestivalService;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.operator.query.application.dto.FieldStaffView;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 권한의 현장 스태프 계정 조회 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FieldStaffQueryApplicationService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalService festivalService;
    private final FieldStaffQueryService fieldStaffQueryService;

    /**
     * 1관리자 또는 2관리자가 담당 축제의 현장 스태프 목록을 조회한다.
     */
    public List<FieldStaffView> searchFieldStaff(
            UUID festivalId,
            String keyword,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        Festival festival = festivalService.getByPublicId(festivalId);
        validateManagePermission(adminAccount, festival);

        return fieldStaffQueryService.searchByFestivalId(
                festival.getId(),
                keyword
        );
    }

    /**
     * 1관리자 또는 2관리자가 담당 축제의 현장 스태프를 단건 조회한다.
     */
    public FieldStaffView getFieldStaff(
            UUID festivalId,
            UUID staffId,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        Festival festival = festivalService.getByPublicId(festivalId);
        validateManagePermission(adminAccount, festival);

        return fieldStaffQueryService.getByFestivalIdAndPublicId(
                festival.getId(),
                staffId
        );
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return adminAccountService.getById(principal.adminId());
    }

    private void validateManagePermission(
            AdminAccount adminAccount,
            Festival festival
    ) {
        if (adminFestivalRoleService == null) {
            if (!festival.getId().equals(adminAccount.getFestivalId())
                    || !adminAccount.canManageFieldStaff()) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
            return;
        }

        AdminFestivalRole role = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(
                        adminAccount.getId(),
                        festival.getId()
                );
        if (!role.canManageFieldStaff()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
