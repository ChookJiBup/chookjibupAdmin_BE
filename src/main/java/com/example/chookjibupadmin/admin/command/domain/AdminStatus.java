package com.example.chookjibupadmin.admin.command.domain;

/**
 * 관리자 계정의 사용 가능 상태를 표현한다.
 */
public enum AdminStatus {
    ACTIVE(true),
    SUSPENDED(false),
    DELETED(false);

    private final boolean authenticatable;

    AdminStatus(boolean authenticatable) {
        this.authenticatable = authenticatable;
    }

    /**
     * 로그인과 인증 기반 API 사용이 가능한 상태인지 확인한다.
     */
    public boolean canAuthenticate() {
        return authenticatable;
    }
}
