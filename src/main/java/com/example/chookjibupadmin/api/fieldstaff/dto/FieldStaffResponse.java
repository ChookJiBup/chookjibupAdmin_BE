package com.example.chookjibupadmin.api.fieldstaff.dto;

import com.example.chookjibupadmin.operator.command.domain.FieldStaffStatus;
import com.example.chookjibupadmin.operator.query.application.dto.FieldStaffView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 현장 스태프 계정 조회 응답 DTO이다.
 */
@Schema(description = "현장 스태프 계정 조회 응답")
public record FieldStaffResponse(
        @Schema(description = "외부 노출용 현장 스태프 ID", example = "44444444-4444-4444-4444-444444444444")
        UUID staffId,

        @Schema(description = "현장 스태프 로그인 ID", example = "staff01")
        String loginId,

        @Schema(description = "현장 스태프 이름", example = "김스태프")
        String name,

        @Schema(description = "현장 스태프 전화번호", example = "010-1234-5678")
        String phoneNumber,

        @Schema(description = "계정 유효 시작 시각", example = "2026-10-09T00:00:00")
        LocalDateTime validFrom,

        @Schema(description = "계정 유효 종료 시각", example = "2026-10-18T23:59:59")
        LocalDateTime validUntil,

        @Schema(description = "계정 상태", example = "ACTIVE")
        FieldStaffStatus status
) {

    /**
     * 현장 스태프 조회 결과를 HTTP 응답 DTO로 변환한다.
     */
    public static FieldStaffResponse from(FieldStaffView view) {
        return new FieldStaffResponse(
                view.staffId(),
                view.loginId(),
                view.name(),
                view.phoneNumber(),
                view.validFrom(),
                view.validUntil(),
                view.status()
        );
    }
}
