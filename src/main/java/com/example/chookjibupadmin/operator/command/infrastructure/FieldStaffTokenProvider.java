package com.example.chookjibupadmin.operator.command.infrastructure;

import com.example.chookjibupadmin.auth.command.infrastructure.JwtProperties;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
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

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encode field staff JWT json.", exception);
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
