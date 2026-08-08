package com.example.chookjibupadmin.api.fieldstaff.dto;

import com.example.chookjibupadmin.operator.command.application.dto.UpdateFieldStaffCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 현장 스태프 기본 정보 수정 요청 DTO이다.
 */
@Schema(description = "현장 스태프 기본 정보 수정 요청")
public record UpdateFieldStaffRequest(
        @Schema(description = "현장 스태프 이름", example = "김스태프")
        @NotBlank @Size(max = 100) String name,
        @Schema(description = "휴대전화 번호", example = "010-1234-5678")
        @NotBlank @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$")
        String phoneNumber
) {
    public UpdateFieldStaffCommand toCommand() {
        return new UpdateFieldStaffCommand(name, phoneNumber);
    }
}
