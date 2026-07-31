package com.example.chookjibupadmin.operator.command.application.dto;

import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;

/**
 * 현장 스태프 계정 생성 결과와 최초 임시 비밀번호를 함께 전달한다.
 */
public record CreateFieldStaffResult(
        FieldStaffAccount fieldStaffAccount,
        String temporaryPassword
) {
}
