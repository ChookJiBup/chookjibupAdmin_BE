package com.example.chookjibupadmin.auth.support;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.auth.command.infrastructure.JwtTokenProvider;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.global.security.ApiAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer JWT를 읽어 관리자 인증 주체를 SecurityContext에 저장한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String FIELD_STAFF_API_PREFIX = "/api/field-staff/";
    private static final String INTERNAL_API_PREFIX = "/internal/api/";
    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final JwtTokenProvider jwtTokenProvider;
    private final AdminAccountService adminAccountService;
    private final AdminAuthCookieService authCookieService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri.equals("/api/field-staff")
                || requestUri.startsWith(FIELD_STAFF_API_PREFIX)
                || requestUri.startsWith(INTERNAL_API_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String accessToken = resolveAccessToken(request);

        if (accessToken != null) {
            try {
                AdminPrincipal principal = jwtTokenProvider.parse(accessToken);
                if (!adminAccountService.isAuthenticationValid(
                        principal.adminId(),
                        principal.authVersion()
                )) {
                    throw new CustomException(ErrorCode.AUTH_TOKEN_INVALID);
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority(ADMIN_AUTHORITY))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (CustomException exception) {
                SecurityContextHolder.clearContext();
                request.setAttribute(
                        ApiAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE,
                        exception.getErrorCode()
                );
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveAccessToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (authCookieService.cookieName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
