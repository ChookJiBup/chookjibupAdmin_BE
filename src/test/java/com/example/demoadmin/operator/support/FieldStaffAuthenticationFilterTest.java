package com.example.demoadmin.operator.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.auth.command.infrastructure.JwtProperties;
import com.example.demoadmin.operator.command.domain.FieldStaffAccount;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffName;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.demoadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import com.example.demoadmin.operator.command.infrastructure.FieldStaffTokenProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class FieldStaffAuthenticationFilterTest {

    private final FieldStaffTokenProvider tokenProvider =
            new FieldStaffTokenProvider(new JwtProperties("test-secret-key", 1800));
    private final FieldStaffAuthenticationFilter filter =
            new FieldStaffAuthenticationFilter(tokenProvider);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("현장 스태프 토큰을 SecurityContext 인증 주체로 저장한다")
        void success_DoFilterInternal() throws Exception {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            ReflectionTestUtils.setField(account, "id", 1L);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setMethod("PATCH");
            request.setServletPath(
                    "/api/festivals/festival-id/booths/booth-id/queue-tail"
            );
            request.addHeader(
                    "Authorization",
                    "Bearer " + tokenProvider.createAccessToken(account)
            );

            // when
            filter.doFilter(
                    request,
                    new MockHttpServletResponse(),
                    new MockFilterChain()
            );

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .isInstanceOf(FieldStaffPrincipal.class);
        }

        @Test
        @DisplayName("허용되지 않은 관리자 API에서는 현장 스태프 토큰을 인증하지 않는다")
        void success_DoFilterInternal_AdminApiSkipped() throws Exception {
            // given
            FieldStaffAccount account = fieldStaffAccount();
            ReflectionTestUtils.setField(account, "id", 1L);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setMethod("GET");
            request.setServletPath("/api/admin/managed-festivals");
            request.addHeader(
                    "Authorization",
                    "Bearer " + tokenProvider.createAccessToken(account)
            );

            // when
            filter.doFilter(
                    request,
                    new MockHttpServletResponse(),
                    new MockFilterChain()
            );

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isNull();
        }
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
