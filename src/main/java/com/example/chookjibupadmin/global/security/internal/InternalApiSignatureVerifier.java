package com.example.chookjibupadmin.global.security.internal;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

/**
 * internal API 요청의 HMAC 서명, timestamp, nonce를 검증한다.
 */
@Component
@RequiredArgsConstructor
public class InternalApiSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String LINE_SEPARATOR = "\n";

    private final InternalApiClientProperties properties;
    private final InternalApiNonceRepository nonceRepository;
    private final Clock clock;

    /**
     * 요청 값이 internal API 인증 규격을 만족하는지 검증한다.
     */
    public void verify(InternalApiAuthenticationRequest request) {
        validateRequiredHeaders(request);

        InternalApiClientProperties.Client client =
                properties.clients().get(request.clientId());
        if (client == null || !StringUtils.hasText(client.secret())) {
            throw new CustomException(ErrorCode.INTERNAL_AUTH_FAILED);
        }

        validateTimestamp(request.timestamp());
        validateSignature(request, client);
        validateNonce(request.clientId(), request.nonce());
    }

    private void validateRequiredHeaders(
            InternalApiAuthenticationRequest request
    ) {
        if (!StringUtils.hasText(request.clientId())
                || !StringUtils.hasText(request.timestamp())
                || !StringUtils.hasText(request.nonce())
                || !StringUtils.hasText(request.signature())) {
            throw new CustomException(ErrorCode.INTERNAL_AUTH_FAILED);
        }
    }

    private void validateTimestamp(String timestamp) {
        OffsetDateTime requestTime;
        try {
            requestTime = OffsetDateTime.parse(timestamp);
        } catch (DateTimeParseException exception) {
            throw new CustomException(ErrorCode.INTERNAL_AUTH_FAILED);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        Duration difference = Duration.between(requestTime, now).abs();
        if (difference.compareTo(properties.timestampTolerance()) > 0) {
            throw new CustomException(ErrorCode.INTERNAL_AUTH_FAILED);
        }
    }

    private void validateSignature(
            InternalApiAuthenticationRequest request,
            InternalApiClientProperties.Client client
    ) {
        String canonicalRequest = canonicalRequest(request);
        boolean activeMatched = matches(
                request.signature(),
                client.secret(),
                canonicalRequest
        );
        boolean previousMatched = StringUtils.hasText(client.previousSecret())
                && matches(
                        request.signature(),
                        client.previousSecret(),
                        canonicalRequest
                );

        if (!activeMatched && !previousMatched) {
            throw new CustomException(ErrorCode.INTERNAL_AUTH_FAILED);
        }
    }

    private boolean matches(
            String actualSignature,
            String secret,
            String canonicalRequest
    ) {
        String expectedSignature = hmac(secret, canonicalRequest);
        return MessageDigest.isEqual(
                actualSignature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void validateNonce(
            String clientId,
            String nonce
    ) {
        boolean saved;
        try {
            saved = nonceRepository.saveIfAbsent(
                    clientId,
                    nonce,
                    properties.nonceTtl()
            );
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.INTERNAL_AUTH_FAILED);
        }

        if (!saved) {
            throw new CustomException(ErrorCode.INTERNAL_AUTH_FAILED);
        }
    }

    private String canonicalRequest(InternalApiAuthenticationRequest request) {
        return String.join(
                LINE_SEPARATOR,
                request.method().toUpperCase(),
                request.path(),
                normalizeQueryString(request.queryString()),
                request.timestamp(),
                request.nonce()
        );
    }

    private String normalizeQueryString(String queryString) {
        if (!StringUtils.hasText(queryString)) {
            return "";
        }

        return List.of(queryString.split("&"))
                .stream()
                .filter(StringUtils::hasText)
                .map(this::queryParameter)
                .sorted(Comparator
                        .comparing(QueryParameter::key)
                        .thenComparing(QueryParameter::value))
                .map(parameter -> parameter.key() + "=" + parameter.value())
                .collect(Collectors.joining("&"));
    }

    private QueryParameter queryParameter(String rawParameter) {
        String[] parts = rawParameter.split("=", 2);
        String key = encode(decode(parts[0]));
        String value = parts.length == 2 ? encode(decode(parts[1])) : "";
        return new QueryParameter(key, value);
    }

    private String decode(String value) {
        return UriUtils.decode(value, StandardCharsets.UTF_8);
    }

    private String encode(String value) {
        return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
    }

    private String hmac(
            String secret,
            String canonicalRequest
    ) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            ));
            return Base64.getEncoder().encodeToString(mac.doFinal(
                    canonicalRequest.getBytes(StandardCharsets.UTF_8)
            ));
        } catch (Exception exception) {
            throw new CustomException(ErrorCode.INTERNAL_AUTH_FAILED);
        }
    }

    /**
     * 테스트와 내부 client 구현에서 동일한 서명 생성 기준을 사용할 수 있도록 제공한다.
     */
    public static String createSignature(
            String secret,
            String method,
            String path,
            Map<String, String> queryParameters,
            String timestamp,
            String nonce
    ) {
        String queryString = queryParameters.entrySet()
                .stream()
                .filter(entry -> entry.getKey() != null)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> encodeStatic(entry.getKey())
                        + "="
                        + encodeStatic(Objects.toString(entry.getValue(), "")))
                .collect(Collectors.joining("&"));
        String canonicalRequest = String.join(
                LINE_SEPARATOR,
                method.toUpperCase(),
                path,
                queryString,
                timestamp,
                nonce
        );

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            ));
            return Base64.getEncoder().encodeToString(mac.doFinal(
                    canonicalRequest.getBytes(StandardCharsets.UTF_8)
            ));
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC 서명 생성에 실패했습니다.", exception);
        }
    }

    private static String encodeStatic(String value) {
        return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
    }

    private record QueryParameter(
            String key,
            String value
    ) {
    }
}
