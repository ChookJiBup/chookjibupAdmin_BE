package com.example.demoadmin.operator.command.infrastructure;

import com.example.demoadmin.auth.command.infrastructure.JwtProperties;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.operator.command.domain.FieldStaffAccount;
import com.example.demoadmin.operator.support.FieldStaffPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 현장 스태프 Access Token을 HMAC-SHA256 JWT 형식으로 발급한다.
 */
@Component
@RequiredArgsConstructor
public class FieldStaffTokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SUBJECT_TYPE = "FIELD_STAFF";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtProperties jwtProperties;

    /**
     * 현장 스태프 ID, 축제 ID, 로그인 아이디를 담은 Access Token을 발급한다.
     */
    public String createAccessToken(FieldStaffAccount fieldStaffAccount) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenExpirationSeconds());

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subjectType", SUBJECT_TYPE);
        payload.put("sub", fieldStaffAccount.getId());
        payload.put("festivalId", fieldStaffAccount.getFestivalId());
        payload.put("loginId", fieldStaffAccount.getLoginIdValue());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String unsignedToken = encodedHeader + "." + encodedPayload;

        return unsignedToken + "." + sign(unsignedToken);
    }

    /**
     * Access Token 만료 시간을 초 단위로 반환한다.
     */
    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.accessTokenExpirationSeconds();
    }

    /**
     * 현장 스태프 Access Token의 서명과 만료 시간을 검증한다.
     */
    public FieldStaffPrincipal parse(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        String unsignedToken = parts[0] + "." + parts[1];
        if (!MessageDigest.isEqual(
                sign(unsignedToken).getBytes(StandardCharsets.US_ASCII),
                parts[2].getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        JsonNode payload = decodePayload(parts[1]);
        if (!SUBJECT_TYPE.equals(payload.path("subjectType").asText())) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        long expiresAt = payload.path("exp").asLong();
        if (expiresAt <= 0 || Instant.now().getEpochSecond() >= expiresAt) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_EXPIRED);
        }

        long fieldStaffId = payload.path("sub").asLong();
        long festivalId = payload.path("festivalId").asLong();
        String loginId = payload.path("loginId").asText("");
        if (fieldStaffId <= 0 || festivalId <= 0 || loginId.isBlank()) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        return new FieldStaffPrincipal(fieldStaffId, festivalId, loginId);
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encode field staff JWT json.", exception);
        }
    }

    private JsonNode decodePayload(String encodedPayload) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(encodedPayload);
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            byte[] secret = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign field staff JWT.", exception);
        }
    }
}
