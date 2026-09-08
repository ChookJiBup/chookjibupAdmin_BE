package com.example.chookjibupadmin.operator.command.application.dto;

/**
 * 현장 스태프 기본 정보 수정 유스케이스 입력이다.
 */
public record UpdateFieldStaffCommand(
        String name,
        String department,
        String phoneNumber
) {

    /**
     * 근무구역 없이 현장 스태프 수정 입력을 만든다.
     */
    public UpdateFieldStaffCommand(
            String name,
            String phoneNumber
    ) {
        this(name, null, phoneNumber);
    }
}
