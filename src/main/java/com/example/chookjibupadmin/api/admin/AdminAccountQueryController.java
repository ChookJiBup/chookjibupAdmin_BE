package com.example.chookjibupadmin.api.admin;

import com.example.chookjibupadmin.admin.query.application.AdminAccountQueryApplicationService;
import com.example.chookjibupadmin.api.admin.dto.AdminAccountProfileResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 계정 본인 정보 조회 API를 제공한다.
 */
@Tag(name = "Admin Account", description = "관리자 계정 관리 API")
@RestController
@RequestMapping("/api/admin/me")
@RequiredArgsConstructor
public class AdminAccountQueryController {

    private final AdminAccountQueryApplicationService queryService;

    /**
     * 관리자 본인 계정 정보를 조회한다.
     */
    @Operation(summary = "관리자 본인 정보 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ApiResponse<AdminAccountProfileResponse> getMyProfile(
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.ADMIN_ACCOUNT_READ_SUCCESS,
                AdminAccountProfileResponse.from(
                        queryService.getMyProfile(principal)
                )
        );
    }
}
