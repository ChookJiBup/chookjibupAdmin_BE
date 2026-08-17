package com.example.chookjibupadmin.api.admin;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminWithdrawService;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.api.admin.dto.UpdateAdminProfileRequest;
import com.example.chookjibupadmin.auth.command.application.AdminPasswordResetApplicationService;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 계정 본인 정보 변경 API를 제공한다.
 */
@Tag(name = "Admin Account", description = "관리자 계정 관리 API")
@RestController
@RequestMapping("/api/admin/me")
@RequiredArgsConstructor
public class AdminAccountCommandController {

    private final AdminWithdrawService adminWithdrawService;
    private final AdminPasswordResetApplicationService passwordResetService;
    private final AdminAccountService adminAccountService;

    /** 로그인한 관리자 본인의 이름, 과·팀, 직급을 수정한다. */
    @Operation(summary = "관리자 본인 정보 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping
    public ApiResponse<Void> updateProfile(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @RequestBody UpdateAdminProfileRequest request
    ) {
        adminAccountService.updateProfile(
                principal.adminId(),
                AdminName.of(request.name()),
                AdminOrganization.of(request.organization()),
                AdminRank.ofNullable(request.rank())
        );
        return ApiResponse.success(SuccessCode.ADMIN_ACCOUNT_UPDATE_SUCCESS);
    }

    /**
     * 로그인한 관리자 계정의 등록 이메일로 비밀번호 변경 링크를 요청한다.
     */
    @Operation(summary = "로그인 관리자 비밀번호 변경 링크 요청")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/password-reset/request")
    public ApiResponse<Void> requestPasswordReset(
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        passwordResetService.requestForAuthenticatedAdmin(principal);
        return ApiResponse.success(
                SuccessCode.ADMIN_PASSWORD_RESET_REQUEST_SUCCESS
        );
    }

    /**
     * 관리자 본인 계정을 탈퇴 상태로 변경한다.
     */
    @Operation(summary = "관리자 회원탈퇴")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/withdrawal")
    public ApiResponse<Void> withdraw(
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        adminWithdrawService.withdraw(principal);
        return ApiResponse.success(SuccessCode.ADMIN_WITHDRAW_SUCCESS);
    }
}
