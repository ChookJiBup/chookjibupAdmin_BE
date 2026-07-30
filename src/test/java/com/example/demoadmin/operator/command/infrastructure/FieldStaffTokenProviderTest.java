package com.example.demoadmin.operator.command.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.auth.command.infrastructure.JwtProperties;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.operator.command.domain.FieldStaffAccount;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffName;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import com.example.demoadmin.operator.support.FieldStaffPrincipal;
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

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        @DisplayName("유효한 현장 스태프 토큰을 인증 주체로 변환한다")
        void success_Parse() {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            ReflectionTestUtils.setField(account, "id", 1L);
            String accessToken = tokenProvider.createAccessToken(account);

            // when
            FieldStaffPrincipal principal = tokenProvider.parse(accessToken);

            // then
            assertThat(principal.fieldStaffId()).isEqualTo(1L);
            assertThat(principal.festivalId()).isEqualTo(10L);
            assertThat(principal.loginId()).isEqualTo("staff01");
        }

        @Test
        @DisplayName("서명이 변경된 토큰은 인증할 수 없다")
        void fail_Parse_CustomException_InvalidSignature() {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            ReflectionTestUtils.setField(account, "id", 1L);
            String accessToken = tokenProvider.createAccessToken(account);
            String tamperedToken = accessToken + "x";

            // when & then
            assertThatThrownBy(() -> tokenProvider.parse(tamperedToken))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_TOKEN_INVALID.getMessage());
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
