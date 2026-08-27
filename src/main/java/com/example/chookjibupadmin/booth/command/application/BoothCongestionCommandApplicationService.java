package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.booth.command.application.dto.BoothCongestionResult;
import com.example.chookjibupadmin.booth.command.application.dto.UpdateBoothCongestionCommand;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 승인된 부스에 혼잡 이력을 append한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BoothCongestionCommandApplicationService {

    private final FestivalService festivalService;
    private final BoothInfoService boothInfoService;
    private final BoothCongestionService boothCongestionService;
    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;

    public BoothCongestionResult record(
            UUID festivalPublicId,
            Long boothId,
            UpdateBoothCongestionCommand command,
            FestivalActorPrincipal principal
    ) {
        Festival festival = festivalService.getByPublicId(festivalPublicId);
        BoothInfo booth = boothInfoService.getById(boothId);
        if (!booth.belongsTo(festival.getId())) {
            throw new CustomException(ErrorCode.BOOTH_NOT_FOUND);
        }

        BoothCongestion saved = switch (principal) {
            case AdminPrincipal adminPrincipal -> recordAsAdmin(
                    festival.getId(),
                    boothId,
                    command,
                    adminPrincipal
            );
            case FieldStaffPrincipal staffPrincipal -> recordAsStaff(
                    festival.getId(),
                    boothId,
                    command,
                    staffPrincipal
            );
            default -> throw new CustomException(ErrorCode.UNAUTHORIZED);
        };
        return BoothCongestionResult.from(saved);
    }

    private BoothCongestion recordAsAdmin(
            Long festivalId,
            Long boothId,
            UpdateBoothCongestionCommand command,
            AdminPrincipal principal
    ) {
        AdminAccount admin = adminAccountService.getById(principal.adminId());
        AdminFestivalRole role = adminFestivalRoleService
                .getByAdminAccountIdAndFestivalId(admin.getId(), festivalId);
        if (!role.canUpdateQueueTail()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return boothCongestionService.save(
                BoothCongestion.recordByAdmin(
                        boothId,
                        admin.getId(),
                        command.waitMinutes(),
                        command.congestionLevel()
                )
        );
    }

    private BoothCongestion recordAsStaff(
            Long festivalId,
            Long boothId,
            UpdateBoothCongestionCommand command,
            FieldStaffPrincipal principal
    ) {
        if (!festivalId.equals(principal.festivalId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return boothCongestionService.save(
                BoothCongestion.recordByStaff(
                        boothId,
                        principal.fieldStaffId(),
                        command.waitMinutes(),
                        command.congestionLevel()
                )
        );
    }
}
