package com.example.chookjibupadmin.global.security.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class InternalApiRedisNonceRepositoryTest {

    private static final String KEY =
            "internal-auth:nonce:chookjibup-user-server:nonce-1";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Nested
    @DisplayName("saveIfAbsent")
    class SaveIfAbsent {

        @Test
        @DisplayName("처음 사용한 nonce면 TTL과 함께 저장하고 true를 반환한다")
        void success_SaveIfAbsent() {
            // given
            InternalApiRedisNonceRepository repository = repository();
            Duration ttl = Duration.ofMinutes(10);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.setIfAbsent(KEY, "1", ttl)).willReturn(true);

            // when
            boolean result = repository.saveIfAbsent(
                    "chookjibup-user-server",
                    "nonce-1",
                    ttl
            );

            // then
            assertThat(result).isTrue();
            then(valueOperations).should().setIfAbsent(KEY, "1", ttl);
        }

        @Test
        @DisplayName("이미 사용한 nonce면 false를 반환한다")
        void success_SaveIfAbsent_ReusedNonceBoundary() {
            // given
            InternalApiRedisNonceRepository repository = repository();
            Duration ttl = Duration.ofMinutes(10);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.setIfAbsent(KEY, "1", ttl)).willReturn(false);

            // when
            boolean result = repository.saveIfAbsent(
                    "chookjibup-user-server",
                    "nonce-1",
                    ttl
            );

            // then
            assertThat(result).isFalse();
        }
    }

    private InternalApiRedisNonceRepository repository() {
        return new InternalApiRedisNonceRepository(redisTemplate);
    }
}
