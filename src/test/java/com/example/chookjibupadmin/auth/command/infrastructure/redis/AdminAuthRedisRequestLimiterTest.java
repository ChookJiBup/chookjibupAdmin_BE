package com.example.chookjibupadmin.auth.command.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class AdminAuthRedisRequestLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("Redis 스크립트가 허용하면 인증 메일 요청을 허용한다")
    void success_TryAcquire_Allowed() {
        // given
        given(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                eq("600000"),
                eq("5"),
                eq("60000")
        )).willReturn(1L);
        AdminAuthRedisRequestLimiter limiter =
                new AdminAuthRedisRequestLimiter(redisTemplate);

        // when
        boolean result = limiter.tryAcquire(
                "password-reset",
                AdminEmail.of("admin@mapo.go.kr"),
                5,
                Duration.ofMinutes(10),
                Duration.ofMinutes(1)
        );

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Redis 스크립트가 거절하면 인증 메일 요청을 제한한다")
    void success_TryAcquire_RateLimited() {
        // given
        given(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(),
                any(),
                any()
        )).willReturn(0L);
        AdminAuthRedisRequestLimiter limiter =
                new AdminAuthRedisRequestLimiter(redisTemplate);

        // when
        boolean result = limiter.tryAcquire(
                "email-verification",
                AdminEmail.of("admin@mapo.go.kr"),
                5,
                Duration.ofMinutes(10),
                Duration.ofMinutes(1)
        );

        // then
        assertThat(result).isFalse();
    }
}
