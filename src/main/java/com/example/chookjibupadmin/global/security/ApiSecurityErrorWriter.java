package com.example.chookjibupadmin.global.security;

import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Spring Security 단계의 인증·인가 실패를 표준 API 응답으로 작성한다.
 */
@Component
@RequiredArgsConstructor
public class ApiSecurityErrorWriter {

    private final ObjectMapper objectMapper;

    /**
     * 지정된 보안 오류를 아직 커밋되지 않은 HTTP 응답에 작성한다.
     */
    public void write(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }
}
