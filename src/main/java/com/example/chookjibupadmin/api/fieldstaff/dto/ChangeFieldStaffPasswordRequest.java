package com.example.chookjibupadmin.api.fieldstaff.dto;

import com.example.chookjibupadmin.operator.command.application.dto.ChangeFieldStaffPasswordCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 현장 스태프 본인 비밀번호 변경 요청이다.
 *
 * <p>새 비밀번호 길이 규칙은 관리자 비밀번호와 같은 기준(8~100자)을 쓴다.</p>
 */
@Schema(description = "현장 스태프 비밀번호 변경 요청")
public record ChangeFieldStaffPasswordRequest(
        @Schema(description = "현재 비밀번호", example = "aB23!cdEF#45")
        @NotBlank
        String currentPassword,

        @Schema(description = "새 비밀번호", example = "NewPassword!123")
        @NotBlank
        @Size(min = 8, max = 100)
        String newPassword
) {

    /**
     * HTTP 요청을 비밀번호 변경 Command로 변환한다.
     */
    public ChangeFieldStaffPasswordCommand toCommand() {
        return new ChangeFieldStaffPasswordCommand(
                currentPassword,
                newPassword
        );
    }
}
