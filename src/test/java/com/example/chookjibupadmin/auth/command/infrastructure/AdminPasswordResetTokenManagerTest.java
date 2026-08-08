package com.example.chookjibupadmin.auth.command.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminPasswordResetTokenManagerTest {

    private final AdminPasswordResetTokenManager tokenManager =
            new AdminPasswordResetTokenManager();

    @Test
    @DisplayName("재설정 토큰은 URL-safe 난수이며 저장용 해시는 원문과 다르다")
    void success_GenerateAndHash() {
        String first = tokenManager.generate();
        String second = tokenManager.generate();

        assertThat(first).matches("^[A-Za-z0-9_-]{43}$");
        assertThat(second).isNotEqualTo(first);
        assertThat(tokenManager.hash(first))
                .hasSize(64)
                .isNotEqualTo(first)
                .isEqualTo(tokenManager.hash(first));
    }
}
