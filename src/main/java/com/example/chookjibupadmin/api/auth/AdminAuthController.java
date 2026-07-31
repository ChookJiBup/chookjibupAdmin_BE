package com.example.chookjibupadmin.api.auth;

import com.example.chookjibupadmin.api.auth.dto.AdminEmailVerificationConfirmRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminEmailVerificationRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminLoginRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminLoginResponse;
import com.example.chookjibupadmin.api.auth.dto.AdminSignupRequest;
import com.example.chookjibupadmin.api.auth.dto.AdminSignupResponse;
import com.example.chookjibupadmin.auth.command.application.AdminEmailVerificationApplicationService;
import com.example.chookjibupadmin.auth.command.application.AdminLoginService;
import com.example.chookjibupadmin.auth.command.application.AdminSignupService;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 회원가입과 로그인 API를 제공한다.
 */
@Tag(name = "Admin Auth", description = "관리자 회원가입 및 JWT 로그인 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminSignupService adminSignupService;
    private final AdminLoginService adminLoginService;
    private final AdminEmailVerificationApplicationService emailVerificationService;

    /**
     * 정부 공식 이메일로 회원가입 인증 코드를 발송한다.
     */
    @Operation(summary = "관리자 이메일 인증 코드 요청")
    @PostMapping("/auth/email-verification/request")
    public ApiResponse<Void> requestEmailVerification(
            @Valid @RequestBody AdminEmailVerificationRequest request
    ) {
        emailVerificationService.request(request);
        return ApiResponse.success(
                SuccessCode.ADMIN_EMAIL_VERIFICATION_REQUEST_SUCCESS
        );
    }

    /**
     * 발송된 이메일 인증 코드를 확인한다.
     */
    @Operation(summary = "관리자 이메일 인증 코드 확인")
    @PostMapping("/auth/email-verification/confirm")
    public ApiResponse<Void> confirmEmailVerification(
            @Valid @RequestBody AdminEmailVerificationConfirmRequest request
    ) {
        emailVerificationService.confirm(request);
        return ApiResponse.success(
                SuccessCode.ADMIN_EMAIL_VERIFICATION_CONFIRM_SUCCESS
        );
    }

    /**
     * 관리자 계정을 생성한다.
     */
    @Operation(summary = "관리자 회원가입")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/auth/signup")
    public ApiResponse<AdminSignupResponse> signup(
            @Valid @RequestBody AdminSignupRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.ADMIN_SIGNUP_SUCCESS,
                adminSignupService.signup(request)
        );
    }

    /**
     * 관리자 이메일과 비밀번호를 검증하고 JWT를 발급한다.
     */
    @Operation(summary = "관리자 로그인")
    @PostMapping("/auth/login")
    public ApiResponse<AdminLoginResponse> login(
            @Valid @RequestBody AdminLoginRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.ADMIN_LOGIN_SUCCESS,
                adminLoginService.login(request)
        );
    }

}

