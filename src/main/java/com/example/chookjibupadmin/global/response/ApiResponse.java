package com.example.chookjibupadmin.global.response;

/**
 * API 성공과 실패 응답을 동일한 형태로 감싸는 공통 응답 모델이다.
 */
public record ApiResponse<T>(
        int code,
        String message,
        T data
) {

    /**
     * 성공 코드와 응답 데이터로 성공 응답을 생성한다.
     */
    public static <T> ApiResponse<T> success(
            SuccessCode successCode,
            T data
    ) {
        return new ApiResponse<>(
                successCode.getCode(),
                successCode.getMessage(),
                data
        );
    }

    /**
     * 성공 코드만으로 데이터 없는 성공 응답을 생성한다.
     */
    public static ApiResponse<Void> success(SuccessCode successCode) {
        return new ApiResponse<>(
                successCode.getCode(),
                successCode.getMessage(),
                null
        );
    }

    /**
     * 에러 코드로 실패 응답을 생성한다.
     */
    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }
}
