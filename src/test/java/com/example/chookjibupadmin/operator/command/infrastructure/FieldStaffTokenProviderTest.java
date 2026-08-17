package com.example.chookjibupadmin.operator.command.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.command.infrastructure.JwtProperties;
import com.example.chookjibupadmin.auth.command.infrastructure.JwtTokenProvider;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FieldStaffTokenProviderTest {

    private static final String SECRET = "test-secret-key";
    private static final Instant NOW = Instant.parse("2026-10-10T00:00:00Z");

    private final FieldStaffTokenProvider tokenProvider =
            tokenProvider(Clock.fixed(NOW, ZoneOffset.UTC));

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
            assertThat(payload.path("authVersion").asLong()).isZero();
            assertThat(tokenProvider.getAccessTokenExpirationSeconds()).isEqualTo(1800L);
        }
    }

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        @DisplayName("유효한 현장 스태프 토큰에서 인증 주체를 복원한다")
        void success_Parse_ValidToken() {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            ReflectionTestUtils.setField(account, "id", 1L);
            String token = tokenProvider.createAccessToken(account);

            // when
            FieldStaffPrincipal principal = tokenProvider.parse(token);

            // then
            assertThat(principal).isEqualTo(new FieldStaffPrincipal(
                    1L,
                    10L,
                    "staff01",
                    0L
            ));
        }

        @Test
        @DisplayName("서명이 변조된 토큰은 거부한다")
        void fail_Parse_TamperedSignature_CustomException() {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            ReflectionTestUtils.setField(account, "id", 1L);
            String token = tokenProvider.createAccessToken(account);
            char replacement = token.endsWith("x") ? 'y' : 'x';
            String tamperedToken = token.substring(0, token.length() - 1)
                    + replacement;

            // when & then
            assertThatThrownBy(() -> tokenProvider.parse(tamperedToken))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        }

        @Test
        @DisplayName("만료 시각과 현재 시각이 같으면 거부한다")
        void fail_Parse_ExpiredBoundary_CustomException() {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            ReflectionTestUtils.setField(account, "id", 1L);
            String token = tokenProvider.createAccessToken(account);
            FieldStaffTokenProvider expiredProvider = tokenProvider(
                    Clock.fixed(NOW.plusSeconds(1800), ZoneOffset.UTC)
            );

            // when & then
            assertThatThrownBy(() -> expiredProvider.parse(token))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_TOKEN_EXPIRED.getMessage());
        }

        @Test
        @DisplayName("관리자 주체 토큰은 현장 스태프 인증으로 인정하지 않는다")
        void fail_Parse_AdminSubject_CustomException() {
            // given
            AdminAccount adminAccount = AdminAccount.createAdmin(
                    AdminEmail.of("admin@mapo.go.kr"),
                    AdminName.of("홍길동"),
                    AdminOrganization.of("관광정책과"),
                    AdminRank.of("주무관"),
                    AdminPasswordHash.of("password-hash")
            );
            ReflectionTestUtils.setField(adminAccount, "id", 1L);
            String adminToken = new JwtTokenProvider(
                    new JwtProperties(
                            SECRET,
                            1800,
                            "chookjibup_admin_access",
                            false,
                            "Strict"
                    )
            ).createAccessToken(adminAccount);

            // when & then
            assertThatThrownBy(() -> tokenProvider.parse(adminToken))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        }
    }

    private FieldStaffTokenProvider tokenProvider(Clock clock) {
        return new FieldStaffTokenProvider(
                new FieldStaffJwtProperties(SECRET, 1800),
                clock
        );
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
