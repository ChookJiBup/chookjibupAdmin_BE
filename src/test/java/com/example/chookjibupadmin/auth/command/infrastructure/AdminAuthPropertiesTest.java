package com.example.chookjibupadmin.auth.command.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminAuthPropertiesTest {

    @Test
    @DisplayName("인증 요청 제한 횟수는 양수여야 한다")
    void fail_Create_RequestLimitNotPositive_IllegalArgumentException() {
        assertThatThrownBy(() -> new AdminAuthProperties.RequestPolicy(
                0,
                Duration.ofMinutes(10),
                Duration.ofMinutes(1)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("재전송 대기 시간은 요청 제한 구간보다 길 수 없다")
    void fail_Create_CooldownLongerThanWindow_IllegalArgumentException() {
        assertThatThrownBy(() -> new AdminAuthProperties.RequestPolicy(
                5,
                Duration.ofMinutes(1),
                Duration.ofMinutes(2)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("비밀번호 재설정 화면 URL은 필수이다")
    void fail_Create_FrontendUrlBlank_IllegalArgumentException() {
        assertThatThrownBy(() -> new AdminAuthProperties.PasswordReset(
                5,
                Duration.ofMinutes(10),
                Duration.ofMinutes(1),
                Duration.ofMinutes(30),
                " "
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
