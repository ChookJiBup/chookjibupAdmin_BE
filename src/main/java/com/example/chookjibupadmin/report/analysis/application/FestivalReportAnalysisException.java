package com.example.chookjibupadmin.report.analysis.application;

/**
 * 결과 보고서 OpenAI 분석 실패를 표현한다.
 */
public class FestivalReportAnalysisException extends RuntimeException {

    private final String code;
    private final boolean retryable;

    public FestivalReportAnalysisException(
            String code,
            String message,
            boolean retryable,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
        this.retryable = retryable;
    }

    public FestivalReportAnalysisException(
            String code,
            String message,
            boolean retryable
    ) {
        this(code, message, retryable, null);
    }

    public String code() {
        return code;
    }

    public boolean retryable() {
        return retryable;
    }
}
