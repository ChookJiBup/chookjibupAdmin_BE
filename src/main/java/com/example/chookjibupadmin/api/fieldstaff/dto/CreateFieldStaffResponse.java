package com.example.chookjibupadmin.api.fieldstaff.dto;

import com.example.chookjibupadmin.operator.command.application.dto.CreateFieldStaffResult;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 현장 스태프 계정 생성 응답이다.
 */
@Schema(description = "현장 스태프 계정 생성 응답")
public record CreateFieldStaffResponse(
        @Schema(description = "현장 스태프 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID staffId,

        @Schema(description = "로그인 아이디", example = "staff01")
        String loginId,

        @Schema(description = "현장 스태프 사용자명", example = "김스태프")
        String name,

        @Schema(description = "현장 스태프 전화번호", example = "010-1234-5678")
        String phoneNumber,

        @Schema(description = "계정 유효 시작 시각", example = "2026-10-09T00:00:00")
        LocalDateTime validFrom,

        @Schema(description = "계정 유효 종료 시각", example = "2026-10-18T23:59:59.999999999")
        LocalDateTime validUntil,

        @Schema(description = "최초 로그인용 임시 비밀번호. 생성 응답에서만 반환된다.", example = "aB23!cdEF#45")
        String temporaryPassword
) {

    /**
     * 생성 결과를 API 응답으로 변환한다.
     */
    public static CreateFieldStaffResponse from(CreateFieldStaffResult result) {
        FieldStaffAccount fieldStaffAccount = result.fieldStaffAccount();
        return new CreateFieldStaffResponse(
                fieldStaffAccount.getPublicId(),
                fieldStaffAccount.getLoginIdValue(),
                fieldStaffAccount.getNameValue(),
                fieldStaffAccount.getPhoneNumberValue(),
                fieldStaffAccount.getValidFrom(),
                fieldStaffAccount.getValidUntil(),
                result.temporaryPassword()
        );
    }
}
