package com.example.demoadmin.global.response;

/**
 * API 성공 상황별 숫자 코드와 메시지를 정의한다.
 */
public enum SuccessCode {
    OK(20000, "요청이 성공적으로 처리되었습니다."),
    ADMIN_SIGNUP_SUCCESS(21000, "관리자 회원가입이 완료되었습니다."),
    ADMIN_LOGIN_SUCCESS(21001, "관리자 로그인에 성공했습니다."),
    ADMIN_EMAIL_VERIFICATION_REQUEST_SUCCESS(21002, "관리자 이메일 인증 코드가 발송되었습니다."),
    ADMIN_EMAIL_VERIFICATION_CONFIRM_SUCCESS(21003, "관리자 이메일 인증이 완료되었습니다."),
    ADMIN_WITHDRAW_SUCCESS(21004, "관리자 회원탈퇴가 완료되었습니다."),
    ADMIN_SUB_ADMIN_READ_SUCCESS(21005, "서브관리자 조회가 완료되었습니다."),
    ADMIN_SUB_ADMIN_CANDIDATE_READ_SUCCESS(21006, "서브관리자 초대 후보 조회가 완료되었습니다."),
    ADMIN_MANAGED_FESTIVAL_READ_SUCCESS(21007, "관리 축제 조회가 완료되었습니다."),
    FESTIVAL_CREATE_SUCCESS(22000, "축제 기본 정보가 저장되었습니다."),
    FESTIVAL_UPDATE_SUCCESS(22001, "축제 기본 정보가 수정되었습니다."),
    FIELD_STAFF_CREATE_SUCCESS(22002, "현장 스태프 계정이 생성되었습니다."),
    FIELD_STAFF_DELETE_SUCCESS(22003, "현장 스태프 계정이 삭제되었습니다."),
    FIELD_STAFF_READ_SUCCESS(22004, "현장 스태프 계정 조회가 완료되었습니다."),
    BOOTH_CREATE_SUCCESS(22005, "축제 부스가 생성되었습니다."),
    BOOTH_UPDATE_SUCCESS(22006, "축제 부스가 수정되었습니다."),
    BOOTH_READ_SUCCESS(22007, "축제 부스 조회가 완료되었습니다."),
    BOOTH_QUEUE_LINE_CREATE_SUCCESS(22008, "부스 대기 라인이 생성되었습니다."),
    BOOTH_QUEUE_LINE_UPDATE_SUCCESS(22009, "부스 대기 라인이 수정되었습니다."),
    BOOTH_QUEUE_TAIL_UPDATE_SUCCESS(22010, "부스 줄 끝이 갱신되었습니다."),
    MAP_ANALYSIS_SUCCESS(22011, "축제 배치도 분석이 완료되었습니다."),
    MAP_OBJECT_READ_SUCCESS(22012, "축제 배치도 객체 조회가 완료되었습니다."),
    FESTIVAL_DASHBOARD_READ_SUCCESS(23000, "축제 대시보드 조회가 완료되었습니다."),
    FESTIVAL_REPORT_SUMMARY_READ_SUCCESS(24000, "축제 결과 보고서 요약 조회가 완료되었습니다."),
    FIELD_STAFF_LOGIN_SUCCESS(25000, "현장 스태프 로그인에 성공했습니다."),
    INTERNAL_FESTIVAL_READ_SUCCESS(26000, "사용자 서버용 축제 목록 조회가 완료되었습니다.");

    private final int code;
    private final String message;

    SuccessCode(
            int code,
            String message
    ) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
