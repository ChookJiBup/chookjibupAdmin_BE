package com.example.chookjibupadmin.global.response;

import org.springframework.http.HttpStatus;

/**
 * API 실패 상황별 숫자 코드, HTTP 상태, 메시지를 정의한다.
 */
public enum ErrorCode {
    BAD_REQUEST(40000, HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_REQUEST(40001, HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    AUTH_EMAIL_DOMAIN_NOT_ALLOWED(40002, HttpStatus.BAD_REQUEST, "정부 공식 이메일만 사용할 수 있습니다."),
    AUTH_EMAIL_VERIFICATION_NOT_FOUND(40003, HttpStatus.BAD_REQUEST, "이메일 인증 요청을 찾을 수 없습니다."),
    AUTH_EMAIL_VERIFICATION_INVALID(40004, HttpStatus.BAD_REQUEST, "이메일 인증 코드가 올바르지 않습니다."),
    AUTH_EMAIL_VERIFICATION_EXPIRED(40005, HttpStatus.BAD_REQUEST, "이메일 인증 코드가 만료되었습니다."),
    AUTH_EMAIL_NOT_VERIFIED(40006, HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),
    AUTH_PASSWORD_CONFIRM_MISMATCH(40101, HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다."),

    UNAUTHORIZED(40100, HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    AUTH_INVALID_CREDENTIALS(40102, HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTH_TOKEN_INVALID(40103, HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다."),
    AUTH_TOKEN_EXPIRED(40104, HttpStatus.UNAUTHORIZED, "만료된 인증 토큰입니다."),
    INTERNAL_AUTH_FAILED(40106, HttpStatus.UNAUTHORIZED, "내부 서버 인증에 실패했습니다."),

    FORBIDDEN(40300, HttpStatus.FORBIDDEN, "권한이 없습니다."),
    AUTH_ADMIN_INACTIVE(40301, HttpStatus.FORBIDDEN, "활성화되지 않은 관리자 계정입니다."),

    AUTH_EMAIL_DUPLICATED(40901, HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    AUTH_FESTIVAL_OWNER_ALREADY_EXISTS(40902, HttpStatus.CONFLICT, "해당 축제에는 이미 1관리자가 존재합니다."),
    AUTH_ADMIN_ALREADY_ASSIGNED(40903, HttpStatus.CONFLICT, "이미 관리 중인 축제가 있는 관리자입니다."),
    AUTH_ADMIN_ALREADY_WITHDRAWN(40907, HttpStatus.CONFLICT, "이미 탈퇴한 관리자 계정입니다."),

    FESTIVAL_NOT_FOUND(40401, HttpStatus.NOT_FOUND, "축제를 찾을 수 없습니다."),
    FESTIVAL_SERIES_NOT_FOUND(40402, HttpStatus.NOT_FOUND, "축제 묶음을 찾을 수 없습니다."),
    FIELD_STAFF_NOT_FOUND(40403, HttpStatus.NOT_FOUND, "현장 스태프 계정을 찾을 수 없습니다."),
    ADMIN_SUB_ADMIN_NOT_FOUND(40404, HttpStatus.NOT_FOUND, "서브관리자를 찾을 수 없습니다."),
    FESTIVAL_YEAR_ALREADY_EXISTS(40904, HttpStatus.CONFLICT, "해당 축제 묶음에는 이미 같은 연도 축제가 존재합니다."),
    FESTIVAL_YEAR_CANNOT_BE_CHANGED(40905, HttpStatus.CONFLICT, "축제 개최 연도는 수정할 수 없습니다."),
    FESTIVAL_MAP_NOT_FOUND(40405, HttpStatus.NOT_FOUND, "축제 배치도를 찾을 수 없습니다."),
    FESTIVAL_MAP_FILE_EMPTY(40007, HttpStatus.BAD_REQUEST, "축제 배치도 이미지가 비어 있습니다."),
    FESTIVAL_MAP_FILE_TOO_LARGE(41300, HttpStatus.CONTENT_TOO_LARGE, "축제 배치도 이미지 크기가 허용 용량을 초과했습니다."),
    FESTIVAL_MAP_FILE_TYPE_NOT_ALLOWED(41500, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "JPEG 또는 PNG 이미지만 등록할 수 있습니다."),
    FESTIVAL_MAP_IMAGE_INVALID(42200, HttpStatus.UNPROCESSABLE_CONTENT, "축제 배치도 이미지를 읽을 수 없습니다."),
    FESTIVAL_MAP_IMAGE_DIMENSION_INVALID(42201, HttpStatus.UNPROCESSABLE_CONTENT, "축제 배치도 이미지 해상도가 허용 범위를 벗어났습니다."),
    FESTIVAL_MAP_UPLOAD_FAILED(50201, HttpStatus.BAD_GATEWAY, "축제 배치도 이미지 저장에 실패했습니다."),
    FESTIVAL_MAP_DELETE_FAILED(50202, HttpStatus.BAD_GATEWAY, "축제 배치도 이미지 정리에 실패했습니다."),
    FESTIVAL_MAP_STORAGE_NOT_CONFIGURED(50301, HttpStatus.SERVICE_UNAVAILABLE, "축제 배치도 이미지 저장 설정이 필요합니다."),
    FESTIVAL_MAP_INVALID_STATUS(40908, HttpStatus.CONFLICT, "현재 상태에서는 축제 배치도를 변경할 수 없습니다."),
    FESTIVAL_MAP_READ_URL_FAILED(50203, HttpStatus.BAD_GATEWAY, "축제 배치도 조회 URL 생성에 실패했습니다."),
    FESTIVAL_MAP_READ_FAILED(50204, HttpStatus.BAD_GATEWAY, "축제 배치도 분석 이미지 조회에 실패했습니다."),
    MAP_ANALYSIS_JOB_NOT_FOUND(40406, HttpStatus.NOT_FOUND, "도면 분석 작업을 찾을 수 없습니다."),
    FESTIVAL_ROADMAP_NOT_FOUND(40407, HttpStatus.NOT_FOUND, "축제 지도 편집 데이터를 찾을 수 없습니다."),
    FESTIVAL_LOCATION_IN_USE(40909, HttpStatus.CONFLICT, "배치도에서 사용 중인 축제 장소는 삭제할 수 없습니다."),
    FIELD_STAFF_LOGIN_ID_DUPLICATED(40906, HttpStatus.CONFLICT, "이미 사용 중인 현장 스태프 아이디입니다."),
    FIELD_STAFF_INVALID_CREDENTIALS(40105, HttpStatus.UNAUTHORIZED, "현장 스태프 아이디 또는 비밀번호가 올바르지 않습니다."),
    FIELD_STAFF_NOT_ACTIVE(40302, HttpStatus.FORBIDDEN, "사용할 수 없는 현장 스태프 계정입니다."),
    FIELD_STAFF_VALID_PERIOD_EXPIRED(40303, HttpStatus.FORBIDDEN, "현장 스태프 계정 유효기간이 아닙니다."),

    ROAD_ADDRESS_SEARCH_FAILED(50200, HttpStatus.BAD_GATEWAY, "도로명주소 검색에 실패했습니다."),
    ROAD_ADDRESS_API_KEY_NOT_CONFIGURED(50300, HttpStatus.SERVICE_UNAVAILABLE, "도로명주소 검색 설정이 필요합니다."),

    INTERNAL_SERVER_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "서버 에러가 발생하였습니다.");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(
            int code,
            HttpStatus httpStatus,
            String message
    ) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
