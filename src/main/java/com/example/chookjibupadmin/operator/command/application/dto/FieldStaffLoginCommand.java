package com.example.chookjibupadmin.operator.command.application.dto;

import java.util.UUID;

/**
 * 현장 스태프 로그인 유스케이스 입력이다.
 */
public record FieldStaffLoginCommand(
        UUID festivalId,
        String loginId,
        String password
) {
}
