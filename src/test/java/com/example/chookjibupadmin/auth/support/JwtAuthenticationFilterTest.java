package com.example.chookjibupadmin.auth.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.auth.command.infrastructure.JwtTokenProvider;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("Bearer 토큰이 유효하면 인증 주체를 저장한다")
        void success_DoFilterInternal_ValidBearer()
                throws ServletException, IOException {
            // given
            JwtAuthenticationFilter filter =
                    new JwtAuthenticationFilter(jwtTokenProvider);
            MockHttpServletRequest request = request("Bearer access-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();
            AdminPrincipal principal = new AdminPrincipal(
                    1L,
                    10L,
                    "admin@mapo.go.kr",
                    AdminRole.SUB_ADMIN
            );
            given(jwtTokenProvider.parse("access-token")).willReturn(principal);

            // when
            filter.doFilter(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isNotNull();
            assertThat(SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal())
                    .isEqualTo(principal);
            assertThat(SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getAuthorities())
                    .isEmpty();
        }

        @Test
        @DisplayName("Bearer 토큰이 없으면 인증 주체를 저장하지 않는다")
        void success_DoFilterInternal_NoBearerBoundary()
                throws ServletException, IOException {
            // given
            JwtAuthenticationFilter filter =
                    new JwtAuthenticationFilter(jwtTokenProvider);
            MockHttpServletRequest request = request(null);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            // when
            filter.doFilter(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isNull();
            then(jwtTokenProvider).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("유효하지 않은 Bearer 토큰이면 인증 주체를 제거한다")
        void fail_DoFilterInternal_CustomException()
                throws ServletException, IOException {
            // given
            JwtAuthenticationFilter filter =
                    new JwtAuthenticationFilter(jwtTokenProvider);
            MockHttpServletRequest request = request("Bearer invalid-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();
            given(jwtTokenProvider.parse("invalid-token"))
                    .willThrow(new CustomException(ErrorCode.AUTH_TOKEN_INVALID));

            // when
            filter.doFilter(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isNull();
        }
    }

    private MockHttpServletRequest request(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }
}
