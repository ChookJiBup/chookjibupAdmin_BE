package com.example.demoadmin.operator.support;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.operator.command.infrastructure.FieldStaffTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 현장 스태프 Bearer JWT를 검증해 SecurityContext에 저장한다.
 */
@Component
@RequiredArgsConstructor
public class FieldStaffAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final FieldStaffTokenProvider fieldStaffTokenProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.PATCH.matches(request.getMethod())) {
            return true;
        }

        return !request.getServletPath().matches(
                "^/api/festivals/[^/]+/booths/[^/]+/queue-tail$"
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateFieldStaff(request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateFieldStaff(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return;
        }

        try {
            FieldStaffPrincipal principal = fieldStaffTokenProvider.parse(
                    authorization.substring(BEARER_PREFIX.length())
            );
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            Collections.emptyList()
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (CustomException exception) {
            SecurityContextHolder.clearContext();
        }
    }
}
