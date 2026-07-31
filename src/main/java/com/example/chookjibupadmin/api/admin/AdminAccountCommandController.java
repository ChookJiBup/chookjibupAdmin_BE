package com.example.chookjibupadmin.api.admin;

import com.example.chookjibupadmin.admin.command.application.AdminWithdrawService;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
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
