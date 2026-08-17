package com.example.chookjibupadmin.admin.query.application.dto;

import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import java.util.UUID;

/**
 * 서브관리자 초대 후보 조회 결과를 표현한다.
 */
public record AdminSubAdminCandidateView(
        UUID adminId,
        String email,
        String name,
        String organization,
        String rank,
        AdminStatus status
) implements AdminNameEmailSearchTarget {
}
