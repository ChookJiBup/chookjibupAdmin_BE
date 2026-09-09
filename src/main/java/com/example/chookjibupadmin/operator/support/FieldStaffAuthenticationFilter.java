package com.example.chookjibupadmin.operator.support;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
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
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    /** 방문 인원 입력은 관리자 전용이라 스태프 자격으로 대체하면 안 된다. */
    private static final Pattern ADMIN_ONLY_OPERATION_PATH = Pattern.compile(
            "^/api/festivals/[^/]+/operations/visitors(?:/.*)?$"
    );
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String FIELD_STAFF_AUTHORITY = "ROLE_FIELD_STAFF";
    /** 기록을 남기지 않는 메서드. 이 요청은 이미 확인된 관리자 신원을 그대로 쓴다. */
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

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
        Authentication existingAuthentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (existingAuthentication != null && !canUseFieldStaffIdentity(request)) {
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
            if (principal == null) {
                throw new CustomException(ErrorCode.AUTH_TOKEN_INVALID);
            }
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
            if (existingAuthentication != null) {
                // 관리자 토큰이거나 만료된 스태프 토큰이면 이미 확인된 관리자 신원을 유지한다.
                SecurityContextHolder.getContext()
                        .setAuthentication(existingAuthentication);
                filterChain.doFilter(request, response);
                return;
            }
            SecurityContextHolder.clearContext();
            errorWriter.write(
                    response,
                    exception.getErrorCode(),
                    exception.getMessage()
            );
        }
    }

    /**
     * 관리자 인증이 이미 있어도 스태프 자격을 우선할 요청인지 판단한다.
     *
     * <p>관리자 콘솔과 스태프 콘솔이 같은 브라우저에 함께 로그인되어 있으면
     * 두 쿠키가 모두 전송되어 서버가 호출 화면을 구분할 수 없다.
     * 이때 관리자 인증을 그대로 쓰면 스태프가 갱신한 줄끝 이력이
     * 관리자 이름으로 남으므로, 스태프도 쓰는 현장 운영 경로에서는
     * 유효한 스태프 토큰이 있으면 스태프 신원을 우선한다.
     * 다만 방문 인원 입력처럼 관리자 전용 경로는 대체하지 않는다.</p>
     *
     * <p>대체하는 것은 <strong>쓰기 요청뿐</strong>이다. 이름이 잘못 남는 문제는
     * 기록을 남기는 요청에서만 생기는데, 조회까지 스태프로 대체하면 스태프는 담당
     * 축제 하나만 볼 수 있으므로 관리자가 자기 축제 대시보드를 열어도 403이 난다 —
     * 스태프 콘솔을 한 번 열어 본 브라우저에서 담당 축제를 뺀 모든 축제의
     * 대시보드가 «부스 0개»로 보이던 원인이다.</p>
     */
    private boolean canUseFieldStaffIdentity(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod())) {
            return false;
        }
        return !ADMIN_ONLY_OPERATION_PATH.matcher(request.getRequestURI()).matches();
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
