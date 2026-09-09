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
        UsernamePasswordAuthenticationToken adminAuthentication =
                adminAuthentication();
        SecurityContextHolder.getContext().setAuthentication(adminAuthentication);
        MockHttpServletRequest request = request(
                "/api/festivals/festival-id/operations/queues",
                "Bearer admin-token"
        );
        MockFilterChain chain = new MockFilterChain();
        given(tokenProvider.parse("admin-token"))
                .willThrow(new CustomException(ErrorCode.AUTH_TOKEN_INVALID));

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(adminAuthentication);
        then(errorWriter).shouldHaveNoInteractions();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void success_DoFilter_SharedOperationWithBothCredentials_PrefersFieldStaff()
            throws ServletException, IOException {
        // given
        SecurityContextHolder.getContext().setAuthentication(adminAuthentication());
        FieldStaffPrincipal principal = principal();
        MockHttpServletRequest request = request(
                "/api/festivals/festival-id/operations/queues/queue-id",
                "Bearer field-token"
        );
        MockFilterChain chain = new MockFilterChain();
        given(tokenProvider.parse("field-token")).willReturn(principal);

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        then(fieldStaffAccountService).should().validateAuthentication(
                principal,
                NOW
        );
        assertThat(SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal()).isEqualTo(principal);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void success_DoFilter_DashboardReadWithBothCredentials_KeepsAdminAuthentication()
            throws ServletException, IOException {
        // given: 스태프 콘솔을 한 번 열어 본 브라우저로 관리자가 대시보드를 조회한다.
        UsernamePasswordAuthenticationToken adminAuthentication =
                adminAuthentication();
        SecurityContextHolder.getContext().setAuthentication(adminAuthentication);
        MockHttpServletRequest request = request(
                "GET",
                "/api/festivals/festival-id/dashboard",
                "Bearer field-token"
        );
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then: 스태프로 대체하면 담당 축제가 아닌 모든 축제가 403이 된다.
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(adminAuthentication);
        then(tokenProvider).shouldHaveNoInteractions();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void success_DoFilter_AdminOnlyVisitorPath_KeepsAdminAuthentication()
            throws ServletException, IOException {
        // given
        UsernamePasswordAuthenticationToken adminAuthentication =
                adminAuthentication();
        SecurityContextHolder.getContext().setAuthentication(adminAuthentication);
        MockHttpServletRequest request = request(
                "/api/festivals/festival-id/operations/visitors/total",
                "Bearer field-token"
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
    void success_DoFilter_ExpiredFieldStaffToken_KeepsAdminAuthentication()
            throws ServletException, IOException {
        // given
        UsernamePasswordAuthenticationToken adminAuthentication =
                adminAuthentication();
        SecurityContextHolder.getContext().setAuthentication(adminAuthentication);
        MockHttpServletRequest request = request(
                "/api/festivals/festival-id/dashboard",
                "Bearer field-token"
        );
        MockFilterChain chain = new MockFilterChain();
        given(tokenProvider.parse("field-token")).willReturn(principal());
        org.mockito.BDDMockito.willThrow(
                        new CustomException(ErrorCode.FIELD_STAFF_VALID_PERIOD_EXPIRED)
                )
                .given(fieldStaffAccountService)
                .validateAuthentication(principal(), NOW);

        // when
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(adminAuthentication);
        then(errorWriter).shouldHaveNoInteractions();
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
                ErrorCode.AUTH_TOKEN_INVALID,
                ErrorCode.AUTH_TOKEN_INVALID.getMessage()
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

    private UsernamePasswordAuthenticationToken adminAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new AdminPrincipal(1L, "admin@mapo.go.kr"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private FieldStaffPrincipal principal() {
        return new FieldStaffPrincipal(1L, 10L, "staff01", 0L);
    }

    private MockHttpServletRequest request(
            String requestUri,
            String authorization
    ) {
        return request("PUT", requestUri, authorization);
    }

    private MockHttpServletRequest request(
            String method,
            String requestUri,
            String authorization
    ) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(requestUri);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }
}
