package com.example.chookjibupadmin.festival.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.dto.FestivalDeletionTarget;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.FestivalMapPurgeService;
import com.example.chookjibupadmin.operator.command.application.FieldStaffAccountService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 축제 삭제의 준비와 DB 영구 삭제를 각각 원자적으로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FestivalDeletionLifecycleService {

    private final FestivalService festivalService;
    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final FestivalLocationService festivalLocationService;
    private final FieldStaffAccountService fieldStaffAccountService;
    private final FestivalMapPurgeService festivalMapPurgeService;

    public FestivalDeletionTarget beginDeletion(
            UUID festivalPublicId,
            AdminPrincipal principal
    ) {
        Festival festival = authorizedFestivalForUpdate(festivalPublicId, principal);
        return new FestivalDeletionTarget(
                festivalMapPurgeService.beginDeletion(festival.getId())
        );
    }

    public void completeDeletion(
            UUID festivalPublicId,
            AdminPrincipal principal
    ) {
        Festival festival = authorizedFestivalForUpdate(festivalPublicId, principal);
        Long festivalId = festival.getId();

        festivalMapPurgeService.purgeDatabase(festivalId);
        festivalLocationService.deleteAll(
                festivalLocationService.findAllByFestivalId(festivalId)
        );
        fieldStaffAccountService.deleteAllByFestivalId(festivalId);

        List<AdminFestivalRole> roles =
                adminFestivalRoleService.getAllByFestivalId(festivalId);
        adminFestivalRoleService.deleteAll(roles);
        festivalService.delete(festival);
    }

    private Festival authorizedFestivalForUpdate(
            UUID festivalPublicId,
            AdminPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        AdminAccount adminAccount = adminAccountService.getById(principal.adminId());
        Festival festival = festivalService.getByPublicIdForUpdate(festivalPublicId);
        AdminFestivalRole role = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(
                        adminAccount.getId(),
                        festival.getId()
                );
        if (!role.canModifyFestivalInfo()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return festival;
    }
}
