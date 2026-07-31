package com.example.chookjibupadmin.auth.command.domain;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.LocalDateTime;

/**
 * 관리자 회원가입 전 이메일 인증 코드와 인증 상태를 관리한다.
 */
public class AdminEmailVerification {

    private final AdminEmail email;
    private final String code;
    private final LocalDateTime expiresAt;
    private boolean verified;

    private AdminEmailVerification(
            AdminEmail email,
            String code,
            LocalDateTime expiresAt,
            boolean verified
    ) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
        this.verified = verified;
    }

    /**
     * 지정한 이메일과 코드로 새 인증 요청을 생성한다.
     */
    public static AdminEmailVerification issue(
            AdminEmail email,
            String code,
            LocalDateTime expiresAt
    ) {
        return new AdminEmailVerification(email, code, expiresAt, false);
    }

    /**
     * 저장소에 보관된 인증 상태를 복원한다.
     */
    public static AdminEmailVerification restore(
            AdminEmail email,
            String code,
            LocalDateTime expiresAt,
            boolean verified
    ) {
        return new AdminEmailVerification(email, code, expiresAt, verified);
    }

    /**
     * 입력된 인증 코드와 만료 시간을 검증하고 인증 완료 상태로 전환한다.
     */
    public void verify(String requestedCode, LocalDateTime now) {
        if (now.isAfter(expiresAt)) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_VERIFICATION_EXPIRED);
        }

        if (!code.equals(requestedCode)) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_VERIFICATION_INVALID);
        }

        verified = true;
    }

    /**
     * 인증 요청 이메일을 반환한다.
     */
    public AdminEmail getEmail() {
        return email;
    }

    /**
     * 발급된 인증 코드를 반환한다.
     */
    public String getCode() {
        return code;
    }

    /**
     * 인증 코드 만료 시각을 반환한다.
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * 인증 완료 여부를 반환한다.
     */
    public boolean isVerified() {
        return verified;
    }
}
