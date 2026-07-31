package com.example.chookjibupadmin.auth.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.api.auth.dto.AdminLoginRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminLoginResponse;
import com.example.chookjibupadmin.auth.command.infrastructure.JwtTokenProvider;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 이메일/비밀번호 로그인과 JWT 발급을 처리한다.
 */
@Service
@RequiredArgsConstructor
public class AdminLoginService {

    private final AdminAccountService adminAccountService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 활성 관리자 인증에 성공하면 Access Token과 관리자 권한 정보를 반환한다.
     */
    @Transactional(readOnly = true)
    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminEmail email = AdminEmail.of(request.email());
        AdminAccount adminAccount = adminAccountService.getByEmailForLogin(email);

        if (!passwordEncoder.matches(request.password(), adminAccount.getPasswordHashValue())) {
            throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        if (!adminAccount.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }

        String accessToken = jwtTokenProvider.createAccessToken(adminAccount);

        return AdminLoginResponse.of(
                accessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                adminAccount
        );
    }
}

