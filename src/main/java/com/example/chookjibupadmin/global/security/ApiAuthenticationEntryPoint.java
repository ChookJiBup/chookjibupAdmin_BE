package com.example.chookjibupadmin.global.security;

import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증되지 않은 보호 API 요청을 표준 401 응답으로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String ERROR_CODE_ATTRIBUTE =
            ApiAuthenticationEntryPoint.class.getName() + ".errorCode";

    private final ApiSecurityErrorWriter errorWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        Object errorCode = request.getAttribute(ERROR_CODE_ATTRIBUTE);
        errorWriter.write(
                response,
                errorCode instanceof ErrorCode code
                        ? code
                        : ErrorCode.UNAUTHORIZED
        );
    }
}
