package com.example.chookjibupadmin.auth.command.application;

import com.example.chookjibupadmin.admin.command.domain.AccountKind;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.api.auth.dto.AdminContractorSignupRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminSignupRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminSignupResponse;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 회원가입 유스케이스를 처리한다.
 */
@Service
@RequiredArgsConstructor
public class AdminSignupService {

    private final AdminAccountService adminAccountService;
    private final PasswordEncoder passwordEncoder;
    private final AdminEmailVerificationService emailVerificationService;

    /**
     * 공무원 관리자 회원가입을 처리한다.
     */
    @Transactional
    public AdminSignupResponse signupGovernment(AdminSignupRequest request) {
        validatePasswordConfirm(request.password(), request.passwordConfirm());

        AdminEmail email = AdminEmail.of(request.email(), AccountKind.GOVERNMENT);
        ensureEmailAvailable(email);

        AdminAccount adminAccount = AdminAccount.createGovernment(
                email,
                AdminName.of(request.name()),
                AdminOrganization.of(request.organization()),
                AdminRank.of(request.rank()),
                AdminPasswordHash.of(passwordEncoder.encode(request.password()))
        );

        return saveVerifiedAccount(adminAccount, email, AccountKind.GOVERNMENT);
    }

    /**
     * 외부업자 관리자 회원가입을 처리한다.
     */
    @Transactional
    public AdminSignupResponse signupContractor(AdminContractorSignupRequest request) {
        validatePasswordConfirm(request.password(), request.passwordConfirm());

        AdminEmail email = AdminEmail.of(request.email(), AccountKind.CONTRACTOR);
        ensureEmailAvailable(email);

        AdminAccount adminAccount = AdminAccount.createContractor(
                email,
                AdminName.of(request.name()),
                AdminOrganization.of(request.companyName()),
                AdminPasswordHash.of(passwordEncoder.encode(request.password()))
        );

        return saveVerifiedAccount(adminAccount, email, AccountKind.CONTRACTOR);
    }

    /**
     * @deprecated {@link #signupGovernment} 사용
     */
    @Deprecated
    @Transactional
    public AdminSignupResponse signup(AdminSignupRequest request) {
        return signupGovernment(request);
    }

    private void validatePasswordConfirm(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_CONFIRM_MISMATCH);
        }
    }

    private void ensureEmailAvailable(AdminEmail email) {
        if (adminAccountService.existsByEmail(email)) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }
    }

    private AdminSignupResponse saveVerifiedAccount(
            AdminAccount adminAccount,
            AdminEmail email,
            AccountKind accountKind
    ) {
        emailVerificationService.ensureVerified(email, accountKind);
        AdminAccount savedAdminAccount = adminAccountService.save(adminAccount);
        emailVerificationService.consumeVerified(email, accountKind);
        return AdminSignupResponse.from(savedAdminAccount);
    }
}
