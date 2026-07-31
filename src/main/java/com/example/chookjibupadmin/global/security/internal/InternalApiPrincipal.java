package com.example.chookjibupadmin.global.security.internal;

/**
 * internal API를 호출한 서버 인증 주체이다.
 */
public record InternalApiPrincipal(
        String clientId
) {
}
