package com.example.chookjibupadmin.global.response;

/**
 * 비즈니스 실패를 표준 ErrorCode와 함께 전달하는 공통 예외이다.
 */
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
