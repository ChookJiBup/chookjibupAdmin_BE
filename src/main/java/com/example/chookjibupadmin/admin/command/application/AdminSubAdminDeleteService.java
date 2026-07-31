package com.example.chookjibupadmin.admin.command.application;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 제1 관리자가 담당 축제의 제2관리자 권한을 삭제하는 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminSubAdminDeleteService {

    private static final int MAX_BULK_DELETE_SIZE = 100;

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalService festivalService;

    /**
     * 선택한 관리자들의 해당 축제 제2관리자 권한을 한 번에 삭제한다.
     */
    public void deleteAll(
            UUID festivalId,
            List<UUID> adminIds,
            AdminPrincipal principal
    ) {
        AdminAccount owner = findAuthenticatedAdmin(principal);
        Festival festival = festivalService.getByPublicId(festivalId);
        validateOwnerAccess(owner, festival);
        Set<UUID> uniqueAdminIds = validateAdminIds(adminIds);

        List<AdminAccount> subAdmins =
                adminAccountService.getAllSubAdminsByPublicIds(uniqueAdminIds);
        List<Long> accountIds = subAdmins.stream()
                .map(AdminAccount::getId)
                .toList();
        List<AdminFestivalRole> roles = adminFestivalRoleService
                .getAllByAdminAccountIdsAndFestivalId(accountIds, festival.getId());

        validateDeleteTargets(roles, accountIds.size(), owner.getId());
        adminFestivalRoleService.deleteAll(roles);
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return adminAccountService.getById(principal.adminId());
    }

    private void validateOwnerAccess(AdminAccount owner, Festival festival) {
        AdminFestivalRole ownerRole = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(owner.getId(), festival.getId());
        if (!ownerRole.canInviteSubAdmin()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private Set<UUID> validateAdminIds(List<UUID> adminIds) {
        if (adminIds == null
                || adminIds.isEmpty()
                || adminIds.size() > MAX_BULK_DELETE_SIZE
                || adminIds.stream().anyMatch(Objects::isNull)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Set<UUID> uniqueAdminIds = new LinkedHashSet<>(adminIds);
        if (uniqueAdminIds.size() != adminIds.size()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return uniqueAdminIds;
    }

    private void validateDeleteTargets(
            List<AdminFestivalRole> roles,
            int requestedSize,
            Long ownerId
    ) {
        if (roles.size() != requestedSize
                || roles.stream().anyMatch(role ->
                        role.getRole() != AdminRole.SUB_ADMIN
                                || !ownerId.equals(role.getInvitedByAdminId()))) {
            throw new CustomException(ErrorCode.ADMIN_SUB_ADMIN_NOT_FOUND);
        }
    }
}
