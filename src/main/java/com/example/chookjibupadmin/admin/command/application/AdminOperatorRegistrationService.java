package com.example.chookjibupadmin.admin.command.application;

import com.example.chookjibupadmin.admin.command.application.dto.RegisterOperatorResult;
import com.example.chookjibupadmin.admin.command.domain.AccountKind;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.infrastructure.FieldStaffPasswordGenerator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 총괄 관리자가 외부업자 운영자를 등록하거나 기존 계정에 배정한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminOperatorRegistrationService {

    private final AdminAccountService adminAccountService;
    private final AdminFestivalRoleService roleService;
    private final FestivalService festivalService;
    private final PasswordEncoder passwordEncoder;
    private final FieldStaffPasswordGenerator passwordGenerator;

    /**
     * 미가입 이메일이면 외부업자 계정을 생성하고, 기존 외부업자면 운영자로 배정한다.
     */
    public RegisterOperatorResult register(
            UUID festivalId,
            String emailValue,
            String nameValue,
            String companyNameValue,
            AdminPrincipal principal
    ) {
        AdminAccount owner = findOwner(principal, festivalId);
        AdminEmail email = AdminEmail.of(emailValue, AccountKind.CONTRACTOR);

        return adminAccountService.findByEmail(email)
                .map(target -> assignExistingOperator(
                        owner,
                        festivalId,
                        target,
                        email,
                        nameValue,
                        companyNameValue
                ))
                .orElseGet(() -> createAndAssignOperator(
                        owner,
                        festivalId,
                        email,
                        nameValue,
                        companyNameValue
                ));
    }

    private RegisterOperatorResult assignExistingOperator(
            AdminAccount owner,
            UUID festivalId,
            AdminAccount target,
            AdminEmail email,
            String nameValue,
            String companyNameValue
    ) {
        if (!target.isContractor()) {
            throw new CustomException(ErrorCode.AUTH_GOVERNMENT_ACCOUNT_CANNOT_BE_OPERATOR);
        }
        if (!target.isActive()) {
            throw new CustomException(ErrorCode.ADMIN_SUB_ADMIN_NOT_FOUND);
        }

        Festival festival = festivalService.getByPublicId(festivalId);
        roleService.assignSubAdmin(target.getId(), festival.getId(), owner.getId());

        return RegisterOperatorResult.assigned(
                target.getPublicId(),
                email.getValue(),
                target.getNameValue(),
                target.getOrganizationValue()
        );
    }

    private RegisterOperatorResult createAndAssignOperator(
            AdminAccount owner,
            UUID festivalId,
            AdminEmail email,
            String nameValue,
            String companyNameValue
    ) {
        String temporaryPassword = passwordGenerator.generate();
        AdminAccount created = adminAccountService.save(AdminAccount.createContractor(
                email,
                AdminName.of(nameValue),
                AdminOrganization.of(companyNameValue),
                AdminPasswordHash.of(passwordEncoder.encode(temporaryPassword))
        ));

        Festival festival = festivalService.getByPublicId(festivalId);
        roleService.assignSubAdmin(created.getId(), festival.getId(), owner.getId());

        return RegisterOperatorResult.created(
                created.getPublicId(),
                email.getValue(),
                nameValue,
                companyNameValue,
                temporaryPassword
        );
    }

    private AdminAccount findOwner(AdminPrincipal principal, UUID festivalId) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        AdminAccount owner = adminAccountService.getById(principal.adminId());
        if (!owner.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }
        Festival festival = festivalService.getByPublicId(festivalId);
        AdminFestivalRole ownerRole = roleService.getByAdminAccountIdAndFestivalId(
                owner.getId(),
                festival.getId()
        );
        if (!ownerRole.canInviteSubAdmin()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return owner;
    }
}
