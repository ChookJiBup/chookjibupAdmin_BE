package com.example.chookjibupadmin.operator.command.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.auth.command.infrastructure.JwtProperties;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FieldStaffTokenProviderTest {

    private final FieldStaffTokenProvider tokenProvider =
            new FieldStaffTokenProvider(new JwtProperties("test-secret-key", 1800));

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("createAccessToken")
    class CreateAccessToken {

        @Test
        @DisplayName("현장 스태프 토큰에 주체 타입과 축제 ID를 포함한다")
        void success_CreateAccessToken() throws Exception {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            ReflectionTestUtils.setField(account, "id", 1L);

            // when
            String accessToken = tokenProvider.createAccessToken(account);
            JsonNode payload = decodePayload(accessToken);

            // then
            assertThat(payload.path("subjectType").asText()).isEqualTo("FIELD_STAFF");
            assertThat(payload.path("sub").asLong()).isEqualTo(1L);
            assertThat(payload.path("festivalId").asLong()).isEqualTo(10L);
            assertThat(payload.path("loginId").asText()).isEqualTo("staff01");
            assertThat(tokenProvider.getAccessTokenExpirationSeconds()).isEqualTo(1800L);
        }
    }

    private JsonNode decodePayload(String accessToken) throws Exception {
        String encodedPayload = accessToken.split("\\.")[1];
        byte[] json = Base64.getUrlDecoder().decode(encodedPayload);
        return objectMapper.readTree(json);
    }

    private FieldStaffAccount fieldStaffAccount() {
        return FieldStaffAccount.create(
                10L,
                FieldStaffLoginId.of("staff01"),
                FieldStaffName.of("김스태프"),
                FieldStaffPhoneNumber.of("010-1234-5678"),
                FieldStaffPasswordHash.of("encoded-password"),
                LocalDateTime.of(2026, 10, 9, 0, 0),
                LocalDateTime.of(2026, 10, 18, 23, 59)
        );
    }
}
