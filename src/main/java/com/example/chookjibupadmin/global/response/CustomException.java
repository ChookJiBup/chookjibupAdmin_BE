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

    /**
     * 같은 ErrorCode 안에서 상황별로 더 구체적인 메시지를 응답할 때 사용한다.
     */
    public CustomException(
            ErrorCode errorCode,
            String message
    ) {
        super(message == null || message.isBlank() ? errorCode.getMessage() : message);
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
