package com.example.chookjibupadmin.global.security.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

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
class InternalApiAuthenticationFilterTest {

    @Mock
    private InternalApiSignatureVerifier signatureVerifier;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("internal API 요청이면 서명을 검증하고 인증 주체를 저장한다")
        void success_DoFilterInternal()
                throws ServletException, IOException {
            // given
            InternalApiAuthenticationFilter filter = filter();
            MockHttpServletRequest request = internalRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            // when
            filter.doFilter(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isNotNull();
            assertThat(SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getPrincipal())
                    .isEqualTo(new InternalApiPrincipal("chookjibup-user-server"));
            then(signatureVerifier).should().verify(
                    new InternalApiAuthenticationRequest(
                            "GET",
                            "/internal/api/festivals",
                            "status=UPCOMING",
                            "chookjibup-user-server",
                            "2026-07-23T22:00:00+09:00",
                            "nonce-1",
                            "signature"
                    )
            );
        }

        @Test
        @DisplayName("internal API가 아니면 서명을 검증하지 않는다")
        void success_DoFilterInternal_NotInternalApiBoundary()
                throws ServletException, IOException {
            // given
            InternalApiAuthenticationFilter filter = filter();
            MockHttpServletRequest request = new MockHttpServletRequest(
                    "GET",
                    "/api/admin/me"
            );
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            // when
            filter.doFilter(request, response, filterChain);

            // then
            then(signatureVerifier).shouldHaveNoInteractions();
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isNull();
        }

        @Test
        @DisplayName("검증 실패 시 CustomException 응답을 직접 작성한다")
        void fail_DoFilterInternal_CustomException()
                throws ServletException, IOException {
            // given
            InternalApiAuthenticationFilter filter = filter();
            MockHttpServletRequest request = internalRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();
            doThrow(new CustomException(ErrorCode.INTERNAL_AUTH_FAILED))
                    .when(signatureVerifier)
                    .verify(new InternalApiAuthenticationRequest(
                            "GET",
                            "/internal/api/festivals",
                            "status=UPCOMING",
                            "chookjibup-user-server",
                            "2026-07-23T22:00:00+09:00",
                            "nonce-1",
                            "signature"
                    ));

            // when
            filter.doFilter(request, response, filterChain);

            // then
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString())
                    .contains(ErrorCode.INTERNAL_AUTH_FAILED.getMessage());
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isNull();
        }
    }

    private InternalApiAuthenticationFilter filter() {
        return new InternalApiAuthenticationFilter(signatureVerifier);
    }

    private MockHttpServletRequest internalRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/internal/api/festivals"
        );
        request.setQueryString("status=UPCOMING");
        request.addHeader("X-Internal-Client-Id", "chookjibup-user-server");
        request.addHeader("X-Internal-Timestamp", "2026-07-23T22:00:00+09:00");
        request.addHeader("X-Internal-Nonce", "nonce-1");
        request.addHeader("X-Internal-Signature", "signature");
        return request;
    }
}
