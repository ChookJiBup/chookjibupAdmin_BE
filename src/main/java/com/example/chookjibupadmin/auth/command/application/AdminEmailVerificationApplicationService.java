package com.example.chookjibupadmin.auth.command.application;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AccountKind;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.api.auth.dto.AdminEmailVerificationConfirmRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminEmailVerificationRequest;
import com.example.chookjibupadmin.auth.command.application.port.AdminAuthRequestLimiter;
import com.example.chookjibupadmin.auth.command.application.port.AdminAuthPolicy;
import com.example.chookjibupadmin.auth.command.application.port.AdminEmailVerificationSender;
import com.example.chookjibupadmin.auth.command.domain.AdminEmailVerification;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 관리자 회원가입 전 이메일 인증 코드 발급과 확인을 처리한다.
 */
@Service
@RequiredArgsConstructor
public class AdminEmailVerificationApplicationService {

    private static final int CODE_BOUND = 1_000_000;
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final String RATE_LIMIT_ACTION = "email-verification";

    private final AdminAccountService adminAccountService;
    private final AdminEmailVerificationService verificationService;
    private final AdminEmailVerificationSender verificationSender;
    private final AdminAuthRequestLimiter requestLimiter;
    private final AdminAuthPolicy authPolicy;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 계정 종류에 맞는 이메일인지 확인한 뒤 인증 코드를 발급한다.
     */
    public void request(AdminEmailVerificationRequest request) {
        AccountKind accountKind = request.accountKind();
        AdminEmail email = AdminEmail.of(request.email(), accountKind);
        ensureRequestAllowed(email);
        if (adminAccountService.existsByEmail(email)) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }

        String code = generateCode();
        verificationService.save(AdminEmailVerification.issue(
                email,
                accountKind,
                code,
                LocalDateTime.now().plus(CODE_TTL)
        ));
        try {
            verificationSender.send(email, code);
        } catch (RuntimeException exception) {
            verificationService.deleteByEmail(email);
            throw new CustomException(ErrorCode.AUTH_EMAIL_SEND_FAILED);
        }
    }

    /**
     * 사용자가 입력한 이메일 인증 코드를 검증한다.
     */
    public void confirm(AdminEmailVerificationConfirmRequest request) {
        AdminEmail email = AdminEmail.of(request.email());
        AdminEmailVerification verification = verificationService.getByEmail(email);

        try {
            verification.verify(request.code(), LocalDateTime.now());
        } catch (CustomException exception) {
            if (exception.getErrorCode()
                    == ErrorCode.AUTH_EMAIL_VERIFICATION_INVALID) {
                verificationService.save(verification);
            }
            throw exception;
        }
        verificationService.save(verification);
    }

    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(CODE_BOUND));
    }

    private void ensureRequestAllowed(AdminEmail email) {
        AdminAuthPolicy.RequestLimit policy =
                authPolicy.emailVerificationPolicy();
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
}
