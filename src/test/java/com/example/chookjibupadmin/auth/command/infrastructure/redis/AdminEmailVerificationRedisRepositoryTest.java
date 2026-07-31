package com.example.chookjibupadmin.auth.command.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.auth.command.domain.AdminEmailVerification;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AdminEmailVerificationRedisRepositoryTest {

    private static final String KEY = "admin:email-verification:admin@mapo.go.kr";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("만료 시간이 남은 인증 정보를 TTL과 함께 저장한다")
        void success_Save_WithTtl() {
            // given
            AdminEmailVerificationRedisRepository repository = repository();
            AdminEmailVerification verification = verification(
                    LocalDateTime.now().plusMinutes(5),
                    false
            );
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            repository.save(verification);

            // then
            then(valueOperations).should().set(
                    eq(KEY),
                    eq("123456|false|" + verification.getExpiresAt()),
                    any(Duration.class)
            );
        }

        @Test
        @DisplayName("만료 시간이 지난 인증 정보는 Redis에서 삭제한다")
        void success_Save_ExpiredBoundary() {
            // given
            AdminEmailVerificationRedisRepository repository = repository();
            AdminEmailVerification verification = verification(
                    LocalDateTime.now().minusNanos(1),
                    false
            );

            // when
            repository.save(verification);

            // then
            then(redisTemplate).should().delete(KEY);
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("Redis에 저장된 인증 정보를 복원한다")
        void success_FindByEmail() {
            // given
            AdminEmailVerificationRedisRepository repository = repository();
            AdminEmail email = AdminEmail.of("admin@mapo.go.kr");
            LocalDateTime expiresAt = LocalDateTime.of(2026, 7, 18, 12, 5);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(KEY))
                    .willReturn("123456|true|" + expiresAt);

            // when
            Optional<AdminEmailVerification> result =
                    repository.findByEmail(email);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo(email);
            assertThat(result.get().isVerified()).isTrue();
        }

        @Test
        @DisplayName("Redis에 인증 정보가 없으면 빈 결과를 반환한다")
        void success_FindByEmail_NotFoundBoundary() {
            // given
            AdminEmailVerificationRedisRepository repository = repository();
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(KEY)).willReturn(null);

            // when
            Optional<AdminEmailVerification> result =
                    repository.findByEmail(AdminEmail.of("admin@mapo.go.kr"));

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("consumeVerified")
    class ConsumeVerified {

        @Test
        @DisplayName("인증 완료 정보는 삭제하면서 true를 반환한다")
        void success_ConsumeVerified_Verified() {
            // given
            AdminEmailVerificationRedisRepository repository = repository();
            LocalDateTime expiresAt = LocalDateTime.of(2026, 7, 18, 12, 5);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.getAndDelete(KEY))
                    .willReturn("123456|true|" + expiresAt);

            // when
            boolean result = repository.consumeVerified(
                    AdminEmail.of("admin@mapo.go.kr")
            );

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("인증 정보가 없으면 false를 반환한다")
        void success_ConsumeVerified_NotFoundBoundary() {
            // given
            AdminEmailVerificationRedisRepository repository = repository();
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.getAndDelete(KEY)).willReturn(null);

            // when
            boolean result = repository.consumeVerified(
                    AdminEmail.of("admin@mapo.go.kr")
            );

            // then
            assertThat(result).isFalse();
        }
    }

    private AdminEmailVerificationRedisRepository repository() {
        return new AdminEmailVerificationRedisRepository(redisTemplate);
    }

    private AdminEmailVerification verification(
            LocalDateTime expiresAt,
            boolean verified
    ) {
        return AdminEmailVerification.restore(
                AdminEmail.of("admin@mapo.go.kr"),
                "123456",
                expiresAt,
                verified
        );
    }
}
