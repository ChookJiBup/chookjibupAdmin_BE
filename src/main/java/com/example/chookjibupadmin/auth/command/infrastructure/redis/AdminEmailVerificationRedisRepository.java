package com.example.chookjibupadmin.auth.command.infrastructure.redis;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.auth.command.domain.AdminEmailVerification;
import com.example.chookjibupadmin.auth.command.domain.AdminEmailVerificationRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis TTL을 사용해 관리자 이메일 인증 코드를 저장하는 저장소 구현체이다.
 */
@Repository
@RequiredArgsConstructor
public class AdminEmailVerificationRedisRepository
        implements AdminEmailVerificationRepository {

    private static final String KEY_PREFIX = "admin:email-verification:";
    private static final String DELIMITER = "|";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(AdminEmailVerification verification) {
        Duration ttl = Duration.between(
                LocalDateTime.now(),
                verification.getExpiresAt()
        );
        if (ttl.isNegative() || ttl.isZero()) {
            redisTemplate.delete(key(verification.getEmail()));
            return;
        }

        redisTemplate.opsForValue().set(
                key(verification.getEmail()),
                serialize(verification),
                ttl
        );
    }

    @Override
    public Optional<AdminEmailVerification> findByEmail(AdminEmail email) {
        String value = redisTemplate.opsForValue().get(key(email));
        return Optional.ofNullable(value)
                .map(storedValue -> deserialize(email, storedValue));
    }

    @Override
    public boolean consumeVerified(AdminEmail email) {
        String value = redisTemplate.opsForValue().getAndDelete(key(email));
        if (value == null) {
            return false;
        }

        return deserialize(email, value).isVerified();
    }

    private String key(AdminEmail email) {
        return KEY_PREFIX + email.getValue();
    }

    private String serialize(AdminEmailVerification verification) {
        return String.join(
                DELIMITER,
                verification.getCode(),
                Boolean.toString(verification.isVerified()),
                verification.getExpiresAt().toString()
        );
    }

    private AdminEmailVerification deserialize(
            AdminEmail email,
            String value
    ) {
        String[] parts = value.split("\\|", -1);
        return AdminEmailVerification.restore(
                email,
                parts[0],
                LocalDateTime.parse(parts[2]),
                Boolean.parseBoolean(parts[1])
        );
    }
}
