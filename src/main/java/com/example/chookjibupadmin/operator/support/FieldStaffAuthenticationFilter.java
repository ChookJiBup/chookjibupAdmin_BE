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
    /** 스태프 콘솔이 «이 요청은 내가 보낸 것»이라고 알리는 헤더. */
    private static final String CONSOLE_HEADER = "X-Chookjibup-Console";
    private static final String FIELD_STAFF_CONSOLE = "field-staff";

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
     * <p>관리자 콘솔과 스태프 콘솔이 같은 브라우저에 함께 로그인되어 있으면 두 쿠키가
     * 모두 전송되어, 서버는 어느 화면에서 부른 요청인지 쿠키만으로 알 수 없다. 서버가
     * 임의로 한쪽을 고르면 어느 쪽으로 골라도 틀린다 — 관리자를 고르면 스태프가 갱신한
     * 줄끝이 관리자 이름으로 남고, 스태프를 고르면 스태프는 담당 축제 하나만 볼 수 있어
     * 관리자가 다른 축제에서 아무것도 못 하게 된다(대시보드가 403으로 «부스 0개»가 되던
     * 것도, 줄끝 갱신이 «권한이 없습니다»로 막히던 것도 이것 때문이다).</p>
     *
     * <p>어느 화면에서 눌렀는지는 클라이언트만 확실히 아니까 스태프 콘솔이 헤더로
     * 알려 준다. 그 표시가 있을 때만 스태프 신원을 우선한다. 다만 방문 인원 입력처럼
     * 관리자 전용 경로는 표시가 있어도 대체하지 않는다.</p>
     */
    private boolean canUseFieldStaffIdentity(HttpServletRequest request) {
        if (!FIELD_STAFF_CONSOLE.equalsIgnoreCase(request.getHeader(CONSOLE_HEADER))) {
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
