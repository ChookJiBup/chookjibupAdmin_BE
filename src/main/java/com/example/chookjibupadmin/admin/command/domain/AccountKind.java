package com.example.chookjibupadmin.admin.command.domain;

/**
 * 관리자 계정 종류를 표현한다.
 */
public enum AccountKind {

    /** 공무원 계정 — 축제 생성 가능 */
    GOVERNMENT,

    /** 외부업자 계정 — 축제 생성 불가, 운영자로 배정 가능 */
    CONTRACTOR
}
