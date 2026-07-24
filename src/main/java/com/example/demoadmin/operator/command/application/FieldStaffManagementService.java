package com.example.demoadmin.operator.command.application;

import com.example.demoadmin.admin.command.domain.AdminAccount;
import com.example.demoadmin.admin.command.application.AdminAccountService;
import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.festival.command.domain.Festival;
import com.example.demoadmin.festival.command.application.FestivalService;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.operator.command.application.dto.CreateFieldStaffCommand;
import com.example.demoadmin.operator.command.application.dto.CreateFieldStaffResult;
import com.example.demoadmin.operator.command.domain.FieldStaffAccount;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffName;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import com.example.demoadmin.operator.command.infrastructure.FieldStaffPasswordGenerator;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 권한으로 현장 스태프 계정을 추가하고 삭제하는 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FieldStaffManagementService {

    private static final int PRE_OPEN_VALID_DAYS = 7;

    private final FieldStaffAccountService fieldStaffAccountService;
    private final FestivalService festivalService;
    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService adminFestivalRoleService;
    private final PasswordEncoder passwordEncoder;
    private final FieldStaffPasswordGenerator passwordGenerator;

    /**
     * 1관리자 또는 2관리자가 담당 축제의 현장 스태프 계정을 생성한다.
     */
    public CreateFieldStaffResult create(
            UUID festivalId,
            CreateFieldStaffCommand command,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        Festival festival = findFestival(festivalId);
        validateManagePermission(adminAccount, festival);

        FieldStaffLoginId loginId = FieldStaffLoginId.of(command.loginId());
        if (fieldStaffAccountService.existsByFestivalIdAndLoginId(
                festival.getId(),
                loginId
        )) {
            throw new CustomException(ErrorCode.FIELD_STAFF_LOGIN_ID_DUPLICATED);
        }

        String temporaryPassword = passwordGenerator.generate();
        FieldStaffAccount fieldStaffAccount = FieldStaffAccount.create(
                festival.getId(),
                loginId,
                FieldStaffName.of(command.name()),
                FieldStaffPhoneNumber.of(command.phoneNumber()),
                FieldStaffPasswordHash.of(passwordEncoder.encode(temporaryPassword)),
                validFrom(festival),
                validUntil(festival)
        );

        return new CreateFieldStaffResult(
                fieldStaffAccountService.save(fieldStaffAccount),
                temporaryPassword
        );
    }

    /**
     * 1관리자 또는 2관리자가 담당 축제의 현장 스태프 계정을 삭제한다.
     */
    public void delete(
            UUID festivalId,
            UUID fieldStaffId,
            AdminPrincipal principal
    ) {
        AdminAccount adminAccount = findAuthenticatedAdmin(principal);
        Festival festival = findFestival(festivalId);
        validateManagePermission(adminAccount, festival);

        FieldStaffAccount fieldStaffAccount = fieldStaffAccountService.getByPublicId(fieldStaffId);
        if (!festival.getId().equals(fieldStaffAccount.getFestivalId())) {
            throw new CustomException(ErrorCode.FIELD_STAFF_NOT_FOUND);
        }

        fieldStaffAccount.delete();
    }

    private AdminAccount findAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return adminAccountService.getById(principal.adminId());
    }

    private Festival findFestival(UUID festivalId) {
        return festivalService.getByPublicId(festivalId);
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

    private LocalDateTime validFrom(Festival festival) {
        return festival.getStartDate()
                .minusDays(PRE_OPEN_VALID_DAYS)
                .atStartOfDay();
    }

    private LocalDateTime validUntil(Festival festival) {
        return LocalDateTime.of(festival.getEndDate(), LocalTime.MAX);
    }
}
