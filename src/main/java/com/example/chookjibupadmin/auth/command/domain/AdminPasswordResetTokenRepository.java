package com.example.chookjibupadmin.auth.command.domain;

import java.time.Duration;
import java.util.Optional;

/**
 * 관리자 비밀번호 재설정 토큰을 저장하고 한 번만 소비하는 저장소 계약이다.
 */
public interface AdminPasswordResetTokenRepository {

    void save(Long adminAccountId, String tokenHash, Duration ttl);

    Optional<Long> consume(String tokenHash);

    void delete(Long adminAccountId, String tokenHash);
}
