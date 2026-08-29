package com.example.chookjibupadmin.admin.command.domain;

/**
 * 관리자 계정 종류를 표현한다.
 *
 * <p>외부업자({@link #CONTRACTOR})는 축제에 배정되면 제2관리자({@code AdminRole.SUB_ADMIN})와
 * 동일한 축제 권한을 쓴다. 축제 신규 생성은 공무원({@link #GOVERNMENT})만 가능하다.</p>
 */
public enum AccountKind {

    /** 공무원 계정 — 축제 신규 생성 가능. 총괄(OWNER) 또는 제2관리자로 배정 가능 */
    GOVERNMENT,

    /**
     * 외부업자 계정 — 축제 신규 생성 불가.
     * 축제에는 제2관리자(SUB_ADMIN)로만 배정되며, 배정 후 권한은 제2관리자와 동일하다.
     */
    CONTRACTOR
}
