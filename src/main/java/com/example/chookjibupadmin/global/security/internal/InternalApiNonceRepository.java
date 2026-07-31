package com.example.chookjibupadmin.global.security.internal;

import java.time.Duration;

/**
 * internal API nonce 재사용 여부를 저장하고 검증하는 저장소 계약이다.
 */
public interface InternalApiNonceRepository {

    /**
     * nonce가 처음 사용된 값이면 저장하고 true를 반환한다.
     */
    boolean saveIfAbsent(
            String clientId,
            String nonce,
            Duration ttl
    );
}
