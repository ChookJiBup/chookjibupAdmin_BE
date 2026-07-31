package com.example.chookjibupadmin.api.fieldstaff;

import com.example.chookjibupadmin.api.fieldstaff.dto.FieldStaffLoginRequest;
import com.example.chookjibupadmin.api.fieldstaff.dto.FieldStaffLoginResponse;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import com.example.chookjibupadmin.operator.command.application.FieldStaffLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현장 스태프 로그인 API를 제공한다.
 */
@Tag(name = "Field Staff Auth", description = "현장 스태프 JWT 로그인 API")
@RestController
@RequestMapping("/api/field-staff")
@RequiredArgsConstructor
public class FieldStaffAuthController {

    private final FieldStaffLoginService fieldStaffLoginService;

    /**
     * 현장 스태프 아이디와 비밀번호를 검증하고 JWT를 발급한다.
     */
    @Operation(summary = "현장 스태프 로그인")
    @PostMapping("/auth/login")
    public ApiResponse<FieldStaffLoginResponse> login(
            @Valid @RequestBody FieldStaffLoginRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.FIELD_STAFF_LOGIN_SUCCESS,
                FieldStaffLoginResponse.from(
                        fieldStaffLoginService.login(request.toCommand())
                )
        );
    }
}
