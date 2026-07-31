package com.example.chookjibupadmin.auth.command.application;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminDepartment;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
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
     * 회원가입 요청을 검증하고 관리자 계정을 생성한다.
     */
    @Transactional
    public AdminSignupResponse signup(AdminSignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_CONFIRM_MISMATCH);
        }

        AdminEmail email = AdminEmail.of(request.email());
        AdminName name = AdminName.of(request.name());
        AdminOrganization organization = AdminOrganization.of(request.organization());
        AdminDepartment department = AdminDepartment.of(request.department());
        AdminRank rank = AdminRank.of(request.rank());

        if (adminAccountService.existsByEmail(email)) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }

        AdminAccount adminAccount = AdminAccount.createAdmin(
                email,
                name,
                organization,
                department,
                rank,
                AdminPasswordHash.of(passwordEncoder.encode(request.password()))
        );

        emailVerificationService.ensureVerified(email);
        AdminAccount savedAdminAccount = adminAccountService.save(adminAccount);
        emailVerificationService.consumeVerified(email);

        return AdminSignupResponse.from(savedAdminAccount);
    }
}
