package com.example.chookjibupadmin.api.fieldstaff.dto;

import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** 현재 인증된 현장 스태프 세션 응답이다. */
@Schema(description = "현장 스태프 세션 응답")
public record FieldStaffSessionResponse(
        UUID staffId,
        UUID festivalId,
        String loginId,
        String name
) {
    public static FieldStaffSessionResponse of(
            FieldStaffAccount account,
            UUID festivalPublicId
    ) {
        return new FieldStaffSessionResponse(
                account.getPublicId(),
                festivalPublicId,
                account.getLoginIdValue(),
                account.getNameValue()
        );
    }
}
