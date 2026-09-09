package com.example.chookjibupadmin.operator.command.application.dto;

/**
 * 현장 스태프 본인 비밀번호 변경 요청 Command이다.
 */
public record ChangeFieldStaffPasswordCommand(
        String currentPassword,
        String newPassword
) {
}
