package com.example.chookjibupadmin.api.fieldstaff.dto;

import com.example.chookjibupadmin.operator.command.application.dto.CreateFieldStaffCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 현장 스태프 계정 생성 요청이다.
 */
@Schema(description = "현장 스태프 계정 생성 요청")
public record CreateFieldStaffRequest(
        @Schema(description = "축제 안에서 사용할 로그인 아이디", example = "staff01")
        @NotBlank
        @Size(min = 4, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$")
        String loginId,

        @Schema(description = "현장 스태프 사용자명", example = "김스태프")
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "현장 스태프 전화번호", example = "010-1234-5678")
        @NotBlank
        @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$")
        String phoneNumber
) {

    /**
     * HTTP 요청을 현장 스태프 생성 Command로 변환한다.
     */
    public CreateFieldStaffCommand toCommand() {
        return new CreateFieldStaffCommand(
                loginId,
                name,
                phoneNumber
        );
    }
}
