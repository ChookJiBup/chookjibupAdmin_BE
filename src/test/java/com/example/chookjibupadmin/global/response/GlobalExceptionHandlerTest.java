package com.example.chookjibupadmin.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("요청 파라미터 타입 변환 실패를 잘못된 요청으로 응답한다")
    void success_HandleTypeMismatchException() {
        // given
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        // when
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleTypeMismatchException();

        // then
        assertThat(response.getStatusCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST.getHttpStatus());
        assertThat(response.getBody())
                .isEqualTo(ApiResponse.error(ErrorCode.INVALID_REQUEST));
    }
}
