package com.example.chookjibupadmin.auth.command.application;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.auth.command.domain.AdminEmailVerification;
import com.example.chookjibupadmin.auth.command.domain.AdminEmailVerificationRepository;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 관리자 이메일 인증 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
public class AdminEmailVerificationService {

    private final AdminEmailVerificationRepository verificationRepository;

    /**
     * 이메일 인증 요청 정보를 저장한다.
     */
    public void save(AdminEmailVerification verification) {
        verificationRepository.save(verification);
    }

    /**
     * 인증 요청 정보를 조회한다.
     */
    public AdminEmailVerification getByEmail(AdminEmail email) {
        return verificationRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.AUTH_EMAIL_VERIFICATION_NOT_FOUND
                ));
    }

    /**
     * 이메일 발송에 실패한 인증 요청을 제거한다.
     */
    public void deleteByEmail(AdminEmail email) {
        verificationRepository.deleteByEmail(email);
    }

    /**
     * 회원가입 가능 여부 판단을 위해 인증 완료 상태인지 확인한다.
     */
    public void ensureVerified(AdminEmail email) {
        AdminEmailVerification verification = verificationRepository
                .findByEmail(email)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.AUTH_EMAIL_NOT_VERIFIED
                ));

        if (!verification.isVerified()) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        }
    }

    /**
     * 인증 완료 상태를 한 번만 사용하고 제거한다.
     */
    public void consumeVerified(AdminEmail email) {
        if (!verificationRepository.consumeVerified(email)) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        }
    }
}
