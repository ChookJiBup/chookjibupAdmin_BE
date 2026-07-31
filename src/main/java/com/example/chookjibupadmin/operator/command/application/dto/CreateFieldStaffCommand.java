package com.example.chookjibupadmin.operator.command.application.dto;

/**
 * 현장 스태프 계정 생성 유스케이스 입력이다.
 */
public record CreateFieldStaffCommand(
        String loginId,
        String name,
        String phoneNumber
) {
}
