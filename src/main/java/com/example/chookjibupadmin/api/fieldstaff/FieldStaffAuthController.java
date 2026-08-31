package com.example.chookjibupadmin.api.fieldstaff;

import com.example.chookjibupadmin.api.fieldstaff.dto.FieldStaffLoginRequest;
import com.example.chookjibupadmin.api.fieldstaff.dto.FieldStaffLoginResponse;
import com.example.chookjibupadmin.api.fieldstaff.dto.FieldStaffSessionResponse;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import com.example.chookjibupadmin.operator.command.application.FieldStaffLoginService;
import com.example.chookjibupadmin.operator.command.application.FieldStaffAccountService;
import com.example.chookjibupadmin.operator.command.application.dto.FieldStaffLoginResult;
import com.example.chookjibupadmin.operator.support.FieldStaffAuthCookieService;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final FieldStaffAccountService fieldStaffAccountService;
    private final FestivalService festivalService;
    private final FieldStaffAuthCookieService authCookieService;

    /**
     * 현장 스태프 아이디와 비밀번호를 검증하고 JWT를 발급한다.
     */
    @Operation(summary = "현장 스태프 로그인")
    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<FieldStaffLoginResponse>> login(
            @Valid @RequestBody FieldStaffLoginRequest request
    ) {
        FieldStaffLoginResult result = fieldStaffLoginService.login(request.toCommand());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.create(
                        result.accessToken(), result.expiresIn()
                ).toString())
                .body(ApiResponse.success(
                        SuccessCode.FIELD_STAFF_LOGIN_SUCCESS,
                        FieldStaffLoginResponse.from(result)
                ));
    }

    @Operation(summary = "현재 현장 스태프 세션 조회")
    @GetMapping("/auth/me")
    public ApiResponse<FieldStaffSessionResponse> me(
            @AuthenticationPrincipal FieldStaffPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FIELD_STAFF_READ_SUCCESS,
                FieldStaffSessionResponse.of(
                        fieldStaffAccountService.getById(principal.fieldStaffId()),
                        festivalService.getById(principal.festivalId()).getPublicId()
                )
        );
    }

    @Operation(summary = "현장 스태프 로그아웃")
    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.expire().toString())
                .body(ApiResponse.success(SuccessCode.ADMIN_LOGOUT_SUCCESS));
    }
}
