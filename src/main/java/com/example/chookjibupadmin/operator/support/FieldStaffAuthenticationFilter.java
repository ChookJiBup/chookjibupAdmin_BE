package com.example.chookjibupadmin.operator.support;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.security.ApiAuthenticationEntryPoint;
import com.example.chookjibupadmin.global.security.ApiSecurityErrorWriter;
import com.example.chookjibupadmin.operator.command.application.FieldStaffAccountService;
import com.example.chookjibupadmin.operator.command.infrastructure.FieldStaffTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 현장 스태프 Bearer JWT를 검증해 SecurityContext에 인증 주체를 저장한다.
 */
@Component
@RequiredArgsConstructor
public class FieldStaffAuthenticationFilter extends OncePerRequestFilter {

    private static final String FIELD_STAFF_API_PREFIX = "/api/field-staff/";
    private static final Pattern FESTIVAL_OPERATION_PATH = Pattern.compile(
            "^/api/festivals/[^/]+/operations(?:/.*)?$"
    );
    private static final Pattern FESTIVAL_DASHBOARD_PATH = Pattern.compile(
            "^/api/festivals/[^/]+/dashboard$"
    );
    private static final Pattern FESTIVAL_BOOTH_CONGESTION_PATH = Pattern.compile(
            "^/api/festivals/[^/]+/booths/[^/]+/congestion$"
    );
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String FIELD_STAFF_AUTHORITY = "ROLE_FIELD_STAFF";

    private final FieldStaffTokenProvider tokenProvider;
    private final FieldStaffAccountService fieldStaffAccountService;
    private final ApiSecurityErrorWriter errorWriter;
    private final Clock clock;
    private final FieldStaffAuthCookieService authCookieService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        boolean fieldStaffPath = requestUri.equals("/api/field-staff")
                || requestUri.startsWith(FIELD_STAFF_API_PREFIX);
        return !fieldStaffPath
                && !FESTIVAL_OPERATION_PATH.matcher(requestUri).matches()
                && !FESTIVAL_DASHBOARD_PATH.matcher(requestUri).matches()
                && !FESTIVAL_BOOTH_CONGESTION_PATH.matcher(requestUri).matches();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = resolveAccessToken(request);
        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            FieldStaffPrincipal principal = tokenProvider.parse(
                    accessToken
            );
            fieldStaffAccountService.validateAuthentication(
                    principal,
                    LocalDateTime.now(clock)
            );
            request.removeAttribute(
                    ApiAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE
            );
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority(
                                    FIELD_STAFF_AUTHORITY
                            ))
                    )
            );
            filterChain.doFilter(request, response);
        } catch (CustomException exception) {
            SecurityContextHolder.clearContext();
            errorWriter.write(response, exception.getErrorCode());
        }
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
