package com.example.chookjibupadmin.global.security.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalApiSignatureVerifierTest {

    private static final String CLIENT_ID = "chookjibup-user-server";
    private static final String SECRET = "test-secret";
    private static final String PREVIOUS_SECRET = "previous-secret";
    private static final String PATH = "/internal/api/festivals";
    private static final String TIMESTAMP = "2026-07-23T22:00:00+09:00";
    private static final String NONCE = "0f70b939-5e1e-4afd-a7e6-c31a3f4c0a46";

    @Mock
    private InternalApiNonceRepository nonceRepository;

    @Nested
    @DisplayName("verify")
    class Verify {

        @Test
        @DisplayName("유효한 서명과 처음 사용한 nonce면 검증에 성공한다")
        void success_Verify() {
            // given
            InternalApiSignatureVerifier verifier = verifier();
            String signature = signature(SECRET, Map.of(
                    "status", "UPCOMING",
                    "page", "0",
                    "size", "20"
            ));
            given(nonceRepository.saveIfAbsent(
                    eq(CLIENT_ID),
                    eq(NONCE),
                    any(Duration.class)
            )).willReturn(true);

            // when & then
            assertThatCode(() -> verifier.verify(new InternalApiAuthenticationRequest(
                    "GET",
                    PATH,
                    "size=20&status=UPCOMING&page=0",
                    CLIENT_ID,
                    TIMESTAMP,
                    NONCE,
                    signature
            ))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("이전 secret으로 생성한 서명도 rotation 기간에는 검증에 성공한다")
        void success_Verify_PreviousSecret() {
            // given
            InternalApiSignatureVerifier verifier = verifier();
            String signature = signature(PREVIOUS_SECRET, Map.of());
            given(nonceRepository.saveIfAbsent(
                    eq(CLIENT_ID),
                    eq(NONCE),
                    any(Duration.class)
            )).willReturn(true);

            // when & then
            assertThatCode(() -> verifier.verify(new InternalApiAuthenticationRequest(
                    "GET",
                    PATH,
                    null,
                    CLIENT_ID,
                    TIMESTAMP,
                    NONCE,
                    signature
            ))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("timestamp 허용 범위 경계 안이면 검증에 성공한다")
        void success_Verify_TimestampBoundary() {
            // given
            InternalApiSignatureVerifier verifier = verifier();
            String boundaryTimestamp = "2026-07-23T21:55:00+09:00";
            String signature = InternalApiSignatureVerifier.createSignature(
                    SECRET,
                    "GET",
                    PATH,
                    Map.of(),
                    boundaryTimestamp,
                    NONCE
            );
            given(nonceRepository.saveIfAbsent(
                    eq(CLIENT_ID),
                    eq(NONCE),
                    any(Duration.class)
            )).willReturn(true);

            // when & then
            assertThatCode(() -> verifier.verify(new InternalApiAuthenticationRequest(
                    "GET",
                    PATH,
                    null,
                    CLIENT_ID,
                    boundaryTimestamp,
                    NONCE,
                    signature
            ))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("필수 헤더가 누락되면 CustomException을 던진다")
        void fail_Verify_CustomException_MissingHeader() {
            // given
            InternalApiSignatureVerifier verifier = verifier();

            // when & then
            assertThatThrownBy(() -> verifier.verify(new InternalApiAuthenticationRequest(
                    "GET",
                    PATH,
                    null,
                    CLIENT_ID,
                    TIMESTAMP,
                    NONCE,
                    null
            )))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INTERNAL_AUTH_FAILED.getMessage());
        }

        @Test
        @DisplayName("등록되지 않은 client id면 CustomException을 던진다")
        void fail_Verify_CustomException_UnknownClient() {
            // given
            InternalApiSignatureVerifier verifier = verifier();

            // when & then
            assertThatThrownBy(() -> verifier.verify(new InternalApiAuthenticationRequest(
                    "GET",
                    PATH,
                    null,
                    "unknown-client",
                    TIMESTAMP,
                    NONCE,
                    "signature"
            )))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INTERNAL_AUTH_FAILED.getMessage());
        }

        @Test
        @DisplayName("timestamp 허용 범위를 벗어나면 CustomException을 던진다")
        void fail_Verify_CustomException_ExpiredTimestampBoundary() {
            // given
            InternalApiSignatureVerifier verifier = verifier();
            String expiredTimestamp = "2026-07-23T21:54:59+09:00";
            String signature = InternalApiSignatureVerifier.createSignature(
                    SECRET,
                    "GET",
                    PATH,
                    Map.of(),
                    expiredTimestamp,
                    NONCE
            );

            // when & then
            assertThatThrownBy(() -> verifier.verify(new InternalApiAuthenticationRequest(
                    "GET",
                    PATH,
                    null,
                    CLIENT_ID,
                    expiredTimestamp,
                    NONCE,
                    signature
            )))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INTERNAL_AUTH_FAILED.getMessage());
        }

        @Test
        @DisplayName("서명이 일치하지 않으면 CustomException을 던진다")
        void fail_Verify_CustomException_InvalidSignature() {
            // given
            InternalApiSignatureVerifier verifier = verifier();

            // when & then
            assertThatThrownBy(() -> verifier.verify(new InternalApiAuthenticationRequest(
                    "GET",
                    PATH,
                    null,
                    CLIENT_ID,
                    TIMESTAMP,
                    NONCE,
                    "invalid-signature"
            )))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INTERNAL_AUTH_FAILED.getMessage());
        }

        @Test
        @DisplayName("nonce가 재사용되면 CustomException을 던진다")
        void fail_Verify_CustomException_ReusedNonce() {
            // given
            InternalApiSignatureVerifier verifier = verifier();
            String signature = signature(SECRET, Map.of());
            given(nonceRepository.saveIfAbsent(
                    eq(CLIENT_ID),
                    eq(NONCE),
                    any(Duration.class)
            )).willReturn(false);

            // when & then
            assertThatThrownBy(() -> verifier.verify(new InternalApiAuthenticationRequest(
                    "GET",
                    PATH,
                    null,
                    CLIENT_ID,
                    TIMESTAMP,
                    NONCE,
                    signature
            )))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INTERNAL_AUTH_FAILED.getMessage());
        }
    }

    private InternalApiSignatureVerifier verifier() {
        return new InternalApiSignatureVerifier(
                properties(),
                nonceRepository,
                Clock.fixed(
                        Instant.parse("2026-07-23T13:00:00Z"),
                        ZoneId.of("Asia/Seoul")
                )
        );
    }

    private InternalApiClientProperties properties() {
        return new InternalApiClientProperties(
                Map.of(
                        CLIENT_ID,
                        new InternalApiClientProperties.Client(
                                SECRET,
                                PREVIOUS_SECRET
                        )
                ),
                Duration.ofMinutes(5),
                Duration.ofMinutes(10)
        );
    }

    private String signature(
            String secret,
            Map<String, String> queryParameters
    ) {
        return InternalApiSignatureVerifier.createSignature(
                secret,
                "GET",
                PATH,
                queryParameters,
                TIMESTAMP,
                NONCE
        );
    }
}
