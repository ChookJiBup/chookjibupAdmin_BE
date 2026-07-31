package com.example.chookjibupadmin.global.security.internal;

/**
 * internal API HMAC 검증에 필요한 요청 값이다.
 */
public record InternalApiAuthenticationRequest(
        String method,
        String path,
        String queryString,
        String clientId,
        String timestamp,
        String nonce,
        String signature
) {
}
