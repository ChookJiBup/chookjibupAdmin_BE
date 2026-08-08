package com.example.chookjibupadmin.auth.command.application;

import com.example.chookjibupadmin.auth.command.domain.AdminPasswordResetTokenRepository;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 비밀번호 재설정 토큰 Repository 접근을 감싸는 wrapper Service이다.
 */
@Service
@RequiredArgsConstructor
public class AdminPasswordResetTokenService {

    private final AdminPasswordResetTokenRepository tokenRepository;

    public void save(Long adminAccountId, String tokenHash, Duration ttl) {
        tokenRepository.save(adminAccountId, tokenHash, ttl);
    }

    public Long consume(String tokenHash) {
        return tokenRepository.consume(tokenHash)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.AUTH_PASSWORD_RESET_TOKEN_INVALID
                ));
    }

    public void delete(Long adminAccountId, String tokenHash) {
        tokenRepository.delete(adminAccountId, tokenHash);
    }
}
