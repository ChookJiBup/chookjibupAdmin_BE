package com.example.chookjibupadmin.global.response;

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
    ADMIN_SUB_ADMIN_BULK_DELETE_SUCCESS(21008, "제2관리자 권한이 일괄 삭제되었습니다."),
    ADMIN_PASSWORD_RESET_REQUEST_SUCCESS(21009, "비밀번호 재설정 안내 메일 요청이 처리되었습니다."),
    ADMIN_PASSWORD_RESET_CONFIRM_SUCCESS(21010, "관리자 비밀번호가 변경되었습니다."),
    ADMIN_SUB_ADMIN_ASSIGN_SUCCESS(21011, "제2관리자 권한이 부여되었습니다."),
    ADMIN_ACCOUNT_READ_SUCCESS(21012, "관리자 본인 정보 조회가 완료되었습니다."),
    FESTIVAL_CREATE_SUCCESS(22000, "축제 기본 정보가 저장되었습니다."),
    FESTIVAL_UPDATE_SUCCESS(22001, "축제 기본 정보가 수정되었습니다."),
    FIELD_STAFF_CREATE_SUCCESS(22002, "현장 스태프 계정이 생성되었습니다."),
    FIELD_STAFF_DELETE_SUCCESS(22003, "현장 스태프 계정이 삭제되었습니다."),
    FIELD_STAFF_READ_SUCCESS(22004, "현장 스태프 계정 조회가 완료되었습니다."),
    FESTIVAL_SERIES_SEARCH_SUCCESS(22005, "기존 축제 검색이 완료되었습니다."),
    ROAD_ADDRESS_SEARCH_SUCCESS(22006, "도로명주소 검색이 완료되었습니다."),
    FIELD_STAFF_BULK_DELETE_SUCCESS(22007, "현장 스태프 계정이 일괄 삭제되었습니다."),
    FIELD_STAFF_UPDATE_SUCCESS(22015, "현장 스태프 정보가 수정되었습니다."),
    FIELD_STAFF_PASSWORD_REISSUE_SUCCESS(22016, "현장 스태프 임시 비밀번호가 재발급되었습니다."),
    FIELD_STAFF_STATUS_UPDATE_SUCCESS(22017, "현장 스태프 상태가 변경되었습니다."),
    FESTIVAL_DELETE_SUCCESS(22018, "축제가 삭제되었습니다."),
    FESTIVAL_MAP_REPLACE_SUCCESS(22008, "축제 배치도가 교체되었습니다."),
    FESTIVAL_MAP_DELETE_SUCCESS(22009, "축제 배치도가 삭제되었습니다."),
    FESTIVAL_MAP_READ_URL_SUCCESS(22010, "축제 배치도 조회 URL이 생성되었습니다."),
    FESTIVAL_MAP_ANALYSIS_READ_SUCCESS(22011, "축제 도면 분석 상태 조회가 완료되었습니다."),
    FESTIVAL_MAP_EDITOR_READ_SUCCESS(22012, "축제 지도 편집 데이터 조회가 완료되었습니다."),
    FESTIVAL_LOCATION_READ_SUCCESS(22013, "축제 장소 조회가 완료되었습니다."),
    FESTIVAL_MAP_EDITOR_SAVE_SUCCESS(22014, "축제 지도 편집 내용이 저장되었습니다."),
    FESTIVAL_DAILY_VISITOR_COUNT_UPDATE_SUCCESS(22019, "축제 일자별 방문 인원 수가 저장되었습니다."),
    FESTIVAL_TOTAL_VISITOR_COUNT_UPDATE_SUCCESS(22020, "축제 총 방문 인원 수가 저장되었습니다."),
    FESTIVAL_DASHBOARD_READ_SUCCESS(23000, "축제 대시보드 조회가 완료되었습니다."),
    FESTIVAL_REPORT_SUMMARY_READ_SUCCESS(24000, "축제 결과 보고서 요약 조회가 완료되었습니다."),
    FIELD_STAFF_LOGIN_SUCCESS(25000, "현장 스태프 로그인에 성공했습니다."),
    INTERNAL_FESTIVAL_READ_SUCCESS(26000, "사용자 서버용 축제 목록 조회가 완료되었습니다."),
    INTERNAL_FESTIVAL_LOCATION_READ_SUCCESS(26001, "사용자 서버용 축제 장소 조회가 완료되었습니다.");

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
