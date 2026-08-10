package com.example.chookjibupadmin.auth.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.api.auth.dto.AdminPasswordResetConfirmRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminPasswordResetRequest;
import com.example.chookjibupadmin.auth.command.application.port.AdminAuthPolicy;
import com.example.chookjibupadmin.auth.command.application.port.AdminAuthRequestLimiter;
import com.example.chookjibupadmin.auth.command.application.port.AdminPasswordResetEmailSender;
import com.example.chookjibupadmin.auth.command.application.port.AdminPasswordResetTokenCodec;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 관리자 비밀번호 재설정 링크 발급과 새 비밀번호 반영을 처리한다.
 */
@Service
@RequiredArgsConstructor
public class AdminPasswordResetApplicationService {

    private static final String RATE_LIMIT_ACTION = "password-reset";

    private final AdminAccountService adminAccountService;
    private final AdminPasswordResetTokenService tokenService;
    private final AdminPasswordResetTokenCodec tokenCodec;
    private final AdminPasswordResetEmailSender emailSender;
    private final AdminAuthRequestLimiter requestLimiter;
    private final AdminAuthPolicy authPolicy;
    private final PasswordEncoder passwordEncoder;

    /**
     * 계정 존재 여부를 응답으로 노출하지 않고 활성 계정에만 링크를 발송한다.
     */
    public void request(AdminPasswordResetRequest request) {
        AdminEmail email = AdminEmail.of(request.email());
        AdminAuthPolicy.PasswordReset policy = authPolicy.passwordResetPolicy();
        ensureRequestAllowed(email, policy);

        adminAccountService.findByEmail(email)
                .filter(AdminAccount::isActive)
                .ifPresent(account -> issueAndSend(account, policy));
    }

    /**
     * 로그인한 관리자 계정의 등록 이메일로 비밀번호 변경 링크를 발송한다.
     */
    public void requestForAuthenticatedAdmin(AdminPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        AdminAccount adminAccount = adminAccountService.getById(principal.adminId());
        if (!adminAccount.isActive()) {
            throw new CustomException(ErrorCode.AUTH_ADMIN_INACTIVE);
        }

        AdminAuthPolicy.PasswordReset policy = authPolicy.passwordResetPolicy();
        ensureRequestAllowed(adminAccount.getEmail(), policy);
        issueAndSend(adminAccount, policy);
    }

    /**
     * 일회용 토큰을 소비하고 비밀번호를 변경해 기존 JWT를 모두 무효화한다.
     */
    public void confirm(AdminPasswordResetConfirmRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_CONFIRM_MISMATCH);
        }

        String tokenHash = tokenCodec.hash(request.token());
        Long adminAccountId = tokenService.consume(tokenHash);
        AdminAccount adminAccount = adminAccountService.getById(adminAccountId);
        if (!adminAccount.isActive()) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_RESET_TOKEN_INVALID);
        }

        adminAccountService.changePassword(
                adminAccount,
                AdminPasswordHash.of(passwordEncoder.encode(request.password()))
        );
    }

    private void issueAndSend(
            AdminAccount adminAccount,
            AdminAuthPolicy.PasswordReset policy
    ) {
        String rawToken = tokenCodec.generate();
        String tokenHash = tokenCodec.hash(rawToken);
        tokenService.save(adminAccount.getId(), tokenHash, policy.tokenTtl());

        try {
            emailSender.send(
                    adminAccount.getEmail(),
                    resetUrl(policy.frontendUrl(), rawToken)
            );
        } catch (RuntimeException exception) {
            tokenService.delete(adminAccount.getId(), tokenHash);
            throw new CustomException(ErrorCode.AUTH_EMAIL_SEND_FAILED);
        }
    }

    private void ensureRequestAllowed(
            AdminEmail email,
            AdminAuthPolicy.PasswordReset policy
    ) {
        boolean acquired = requestLimiter.tryAcquire(
                RATE_LIMIT_ACTION,
                email,
                policy.requestLimit(),
                policy.requestWindow(),
                policy.resendCooldown()
        );
        if (!acquired) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_REQUEST_LIMIT_EXCEEDED);
        }
    }

    private String resetUrl(String frontendUrl, String token) {
        String delimiter = frontendUrl.contains("?") ? "&" : "?";
        return frontendUrl
                + delimiter
                + "token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
