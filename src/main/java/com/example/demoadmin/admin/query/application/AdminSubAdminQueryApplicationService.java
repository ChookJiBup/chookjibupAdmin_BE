package com.example.demoadmin.admin.query.application;

import com.example.demoadmin.admin.command.application.AdminAccountService;
import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.admin.command.domain.AdminAccount;
import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.admin.query.application.dto.AdminSubAdminView;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.festival.command.application.FestivalService;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 제1 관리자의 서브관리자 조회 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSubAdminQueryApplicationService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalService festivalService;
    private final AdminSubAdminQueryService subAdminQueryService;

    /**
     * 제1 관리자가 담당 축제의 서브관리자 목록을 조회한다.
     */
    public List<AdminSubAdminView> getSubAdmins(
            UUID festivalId,
            String keyword,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        Festival festival = festivalService.getByPublicId(festivalId);
        validateOwnerAccess(adminAccount, festival);

        return subAdminQueryService.searchInvitedSubAdmins(
                festival.getId(),
                adminAccount.getId(),
                keyword
        );
    }

    /**
     * 제1 관리자가 담당 축제의 서브관리자를 외부 UUID로 단건 조회한다.
     */
    public AdminSubAdminView getSubAdmin(
            UUID festivalId,
            UUID adminId,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        Festival festival = festivalService.getByPublicId(festivalId);
        validateOwnerAccess(adminAccount, festival);

        return subAdminQueryService.getInvitedSubAdmin(
                festival.getId(),
                adminAccount.getId(),
                adminId
        );
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return adminAccountService.getById(principal.adminId());
    }

    private void validateOwnerAccess(
            AdminAccount adminAccount,
            Festival festival
    ) {
        if (adminFestivalRoleService == null) {
            if (!festival.getId().equals(adminAccount.getFestivalId())
                    || !adminAccount.canInviteSubAdmin()) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
            return;
        }

        AdminFestivalRole role = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(
                        adminAccount.getId(),
                        festival.getId()
                );
        if (!role.canInviteSubAdmin()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
