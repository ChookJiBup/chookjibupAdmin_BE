package com.example.chookjibupadmin.operator.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.global.security.ApiAuthenticationEntryPoint;
import com.example.chookjibupadmin.global.security.ApiSecurityErrorWriter;
import com.example.chookjibupadmin.operator.command.application.FieldStaffAccountService;
import com.example.chookjibupadmin.operator.command.infrastructure.FieldStaffTokenProvider;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class FieldStaffAuthenticationFilterTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 10, 10, 9, 0);

    @Mock
    private FieldStaffTokenProvider tokenProvider;

    @Mock
    private FieldStaffAccountService fieldStaffAccountService;

    @Mock
    private ApiSecurityErrorWriter errorWriter;

    @Mock
    private FieldStaffAuthCookieService authCookieService;

    private FieldStaffAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-10-10T00:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        filter = new FieldStaffAuthenticationFilter(
                tokenProvider,
                fieldStaffAccountService,
                errorWriter,
                clock,
                authCookieService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void success_DoFilter_ValidFieldStaffToken() throws Exception {
        // given
        FieldStaffPrincipal principal = principal();
        MockHttpServletRequest request = request(
                "/api/field-staff/me",
                "Bearer field-token"
        );
        request.setAttribute(
                ApiAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE,
                ErrorCode.AUTH_TOKEN_INVALID
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        given(tokenProvider.parse("field-token")).willReturn(principal);

        // when
        filter.doFilter(request, response, chain);

        // then
        then(fieldStaffAccountService).should().validateAuthentication(
                principal,
                NOW
        );
        assertThat(SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal()).isEqualTo(principal);
        assertThat(SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_FIELD_STAFF");
        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(request.getAttribute(
                ApiAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE
        )).isNull();
    }

    @Test
    void success_DoFilter_SharedOperationWithAdminAuthentication()
            throws ServletException, IOException {
        // given
        AdminPrincipal adminPrincipal = new AdminPrincipal(
                1L,
                "admin@mapo.go.kr"
        );
        UsernamePasswordAuthenticationToken adminAuthentication =
                new UsernamePasswordAuthenticationToken(
                        adminPrincipal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
        SecurityContextHolder.getContext().setAuthentication(adminAuthentication);
        MockHttpServletRequest request = request(
                "/api/festivals/festival-id/operations/queues",
                "Bearer admin-token"
        );
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(adminAuthentication);
        then(tokenProvider).shouldHaveNoInteractions();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void fail_DoFilter_InvalidFieldStaffToken_WritesError() throws Exception {
        // given
        MockHttpServletRequest request = request(
                "/api/field-staff/me",
                "Bearer invalid-token"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        CustomException exception = new CustomException(ErrorCode.AUTH_TOKEN_INVALID);
        given(tokenProvider.parse("invalid-token")).willThrow(exception);

        // when
        filter.doFilter(request, response, chain);

        // then
        then(errorWriter).should().write(
                response,
                ErrorCode.AUTH_TOKEN_INVALID
        );
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void success_DoFilter_AdminManagementPathBoundary() throws Exception {
        // given
        MockHttpServletRequest request = request(
                "/api/festivals/festival-id/field-staff",
                "Bearer field-token"
        );
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        then(tokenProvider).shouldHaveNoInteractions();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    private FieldStaffPrincipal principal() {
        return new FieldStaffPrincipal(1L, 10L, "staff01", 0L);
    }

    private MockHttpServletRequest request(
            String requestUri,
            String authorization
    ) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(requestUri);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }
}
