package com.example.chookjibupadmin.auth.command.infrastructure;

import com.example.chookjibupadmin.auth.command.application.port.AdminPasswordResetTokenCodec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * URL에 사용할 재설정 토큰을 생성하고 저장용 SHA-256 해시를 만든다.
 */
@Component
public class AdminPasswordResetTokenManager
        implements AdminPasswordResetTokenCodec {

    private static final int TOKEN_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hash(String rawToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash password reset token.", exception);
        }
    }
}
