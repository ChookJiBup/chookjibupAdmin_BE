package com.example.chookjibupadmin.admin.query.application.dto;

import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import java.util.UUID;

/**
 * 관리자 본인 정보 조회 결과를 표현한다.
 */
public record AdminAccountProfileView(
        UUID adminId,
        String email,
        String name,
        String organization,
        String rank,
        AdminStatus status
) {
}
