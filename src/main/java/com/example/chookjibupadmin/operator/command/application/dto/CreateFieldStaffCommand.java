package com.example.chookjibupadmin.operator.command.application.dto;

/**
 * 현장 스태프 계정 생성 유스케이스 입력이다.
 */
public record CreateFieldStaffCommand(
        String loginId,
        String name,
        String department,
        String phoneNumber
) {

    /**
     * 근무구역 없이 현장 스태프 생성 입력을 만든다.
     */
    public CreateFieldStaffCommand(
            String loginId,
            String name,
            String phoneNumber
    ) {
        this(loginId, name, null, phoneNumber);
    }
}
