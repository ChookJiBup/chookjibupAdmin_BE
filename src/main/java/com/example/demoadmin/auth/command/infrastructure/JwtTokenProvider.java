package com.example.demoadmin.auth.command.infrastructure;

import com.example.demoadmin.admin.command.domain.AdminAccount;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 관리자 Access Token을 HMAC-SHA256 JWT 형식으로 발급하고 검증한다.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ADMIN_SUBJECT_TYPE = "ADMIN";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtProperties jwtProperties;

    /**
     * 관리자 계정의 ID와 이메일을 담은 Access Token을 발급한다.
     */
    public String createAccessToken(AdminAccount adminAccount) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenExpirationSeconds());

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subjectType", ADMIN_SUBJECT_TYPE);
        payload.put("sub", adminAccount.getId());
        payload.put("email", adminAccount.getEmailValue());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String unsignedToken = encodedHeader + "." + encodedPayload;

        return unsignedToken + "." + sign(unsignedToken);
    }

    /**
     * Access Token의 서명과 만료 시간을 검증하고 인증 주체를 반환한다.
     */
    public AdminPrincipal parse(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!expectedSignature.equals(parts[2])) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        JsonNode payload = decodePayload(parts[1]);
        validateAdminSubject(payload);
        long exp = payload.path("exp").asLong();
        if (Instant.now().getEpochSecond() > exp) {
            throw new CustomException(ErrorCode.AUTH_TOKEN_EXPIRED);
        }

        return new AdminPrincipal(
                payload.path("sub").asLong(),
                payload.path("email").asText()
        );
    }

    /**
     * Access Token 만료 시간을 초 단위로 반환한다.
     */
    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.accessTokenExpirationSeconds();
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encode JWT json.", exception);
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

    private void validateAdminSubject(JsonNode payload) {
        JsonNode subjectType = payload.get("subjectType");
        if (subjectType != null && !ADMIN_SUBJECT_TYPE.equals(subjectType.asText())) {
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
            throw new IllegalStateException("Failed to sign JWT.", exception);
        }
    }
}
