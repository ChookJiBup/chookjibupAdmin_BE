package com.example.chookjibupadmin.api.fieldstaff.dto;

import com.example.chookjibupadmin.operator.command.application.dto.FieldStaffLoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * 현장 스태프 로그인 요청이다.
 */
@Schema(description = "현장 스태프 로그인 요청")
public record FieldStaffLoginRequest(
        @Schema(description = "축제 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull
        UUID festivalId,

        @Schema(description = "현장 스태프 로그인 아이디", example = "staff01")
        @NotBlank
        @Size(min = 4, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$")
        String loginId,

        @Schema(description = "임시 비밀번호", example = "aB23!cdEF#45")
        @NotBlank
        String password
) {

    /**
     * HTTP 요청을 현장 스태프 로그인 Command로 변환한다.
     */
    public FieldStaffLoginCommand toCommand() {
        return new FieldStaffLoginCommand(
                festivalId,
                loginId,
                password
        );
    }
}
