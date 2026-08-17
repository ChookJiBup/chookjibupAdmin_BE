package com.example.chookjibupadmin.api.admin.dto;

import com.example.chookjibupadmin.admin.command.application.dto.RegisterOperatorResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * 운영자 등록 응답 DTO이다.
 */
@Schema(description = "운영자 등록 응답")
public record RegisterOperatorResponse(
        @Schema(description = "외부 노출용 관리자 ID. 신규 생성 시에만 제공")
        UUID adminId,

        @Schema(description = "운영자 이메일", example = "vendor@example.com")
        String email,

        @Schema(description = "운영자 이름", example = "김운영")
        String name,

        @Schema(description = "업체명", example = "축제기획(주)")
        String companyName,

        @Schema(description = "신규 계정 생성 여부", example = "true")
        boolean created,

        @Schema(description = "신규 생성 시 한 번만 노출되는 임시 비밀번호")
        String temporaryPassword
) {

    public static RegisterOperatorResponse from(RegisterOperatorResult result) {
        return new RegisterOperatorResponse(
                result.adminId(),
                result.email(),
                result.name(),
                result.companyName(),
                result.created(),
                result.temporaryPassword()
        );
    }
}
