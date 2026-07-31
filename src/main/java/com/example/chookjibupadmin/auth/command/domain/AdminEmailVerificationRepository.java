package com.example.chookjibupadmin.auth.command.domain;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import java.util.Optional;

/**
 * 관리자 이메일 인증 상태를 저장하고 조회하는 저장소 계약이다.
 */
public interface AdminEmailVerificationRepository {

    /**
     * 이메일 인증 요청 정보를 저장한다.
     */
    void save(AdminEmailVerification verification);

    /**
     * 이메일 기준으로 인증 요청 정보를 조회한다.
     */
    Optional<AdminEmailVerification> findByEmail(AdminEmail email);

    /**
     * 인증 완료 상태를 한 번만 사용하고 제거한다.
     */
    boolean consumeVerified(AdminEmail email);
}
