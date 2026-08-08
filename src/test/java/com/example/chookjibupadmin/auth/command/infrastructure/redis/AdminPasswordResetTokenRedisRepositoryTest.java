package com.example.chookjibupadmin.auth.command.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AdminPasswordResetTokenRedisRepositoryTest {

    private static final String TOKEN_KEY =
            "admin:password-reset:token:token-hash";
    private static final String ACCOUNT_KEY =
            "admin:password-reset:account:1";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("새 토큰을 저장하면 같은 계정의 이전 토큰을 제거한다")
    void success_Save_ReplacesPreviousToken() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(ACCOUNT_KEY)).willReturn("previous-hash");
        AdminPasswordResetTokenRedisRepository repository = repository();

        // when
        repository.save(1L, "token-hash", Duration.ofMinutes(30));

        // then
        then(redisTemplate).should().delete(
                "admin:password-reset:token:previous-hash"
        );
        then(valueOperations).should().set(
                TOKEN_KEY,
                "1",
                Duration.ofMinutes(30)
        );
        then(valueOperations).should().set(
                ACCOUNT_KEY,
                "token-hash",
                Duration.ofMinutes(30)
        );
    }

    @Test
    @DisplayName("토큰은 조회와 동시에 삭제되어 한 번만 소비된다")
    void success_Consume_OnlyOnce() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.getAndDelete(TOKEN_KEY)).willReturn("1");
        AdminPasswordResetTokenRedisRepository repository = repository();

        // when
        Optional<Long> result = repository.consume("token-hash");

        // then
        assertThat(result).contains(1L);
        then(redisTemplate).should().execute(
                any(),
                eq(List.of(ACCOUNT_KEY)),
                eq("token-hash")
        );
    }

    @Test
    @DisplayName("저장되지 않은 토큰은 소비할 수 없다")
    void success_Consume_NotFoundBoundary() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.getAndDelete(TOKEN_KEY)).willReturn(null);

        // when
        Optional<Long> result = repository().consume("token-hash");

        // then
        assertThat(result).isEmpty();
    }

    private AdminPasswordResetTokenRedisRepository repository() {
        return new AdminPasswordResetTokenRedisRepository(redisTemplate);
    }
}
