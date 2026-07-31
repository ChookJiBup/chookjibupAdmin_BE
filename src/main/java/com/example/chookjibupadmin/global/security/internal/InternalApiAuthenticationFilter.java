package com.example.chookjibupadmin.global.security.internal;

import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * `/internal/api/**` 요청에 서버 간 HMAC 인증을 적용한다.
 */
@Component
@RequiredArgsConstructor
public class InternalApiAuthenticationFilter extends OncePerRequestFilter {

    private static final String INTERNAL_API_PREFIX = "/internal/api/";
    private static final String CLIENT_ID_HEADER = "X-Internal-Client-Id";
    private static final String TIMESTAMP_HEADER = "X-Internal-Timestamp";
    private static final String NONCE_HEADER = "X-Internal-Nonce";
    private static final String SIGNATURE_HEADER = "X-Internal-Signature";

    private final InternalApiSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_API_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            InternalApiAuthenticationRequest authenticationRequest =
                    authenticationRequest(request);
            signatureVerifier.verify(authenticationRequest);
            SecurityContextHolder.getContext().setAuthentication(
                    authentication(authenticationRequest.clientId())
            );
            filterChain.doFilter(request, response);
        } catch (CustomException exception) {
            SecurityContextHolder.clearContext();
            writeErrorResponse(response, exception.getErrorCode());
        }
    }

    private InternalApiAuthenticationRequest authenticationRequest(
            HttpServletRequest request
    ) {
        return new InternalApiAuthenticationRequest(
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                request.getHeader(CLIENT_ID_HEADER),
                request.getHeader(TIMESTAMP_HEADER),
                request.getHeader(NONCE_HEADER),
                request.getHeader(SIGNATURE_HEADER)
        );
    }

    private UsernamePasswordAuthenticationToken authentication(String clientId) {
        return new UsernamePasswordAuthenticationToken(
                new InternalApiPrincipal(clientId),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_API"))
        );
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }
}
