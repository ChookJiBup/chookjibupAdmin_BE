package com.example.chookjibupadmin.api.admin;

import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.query.application.AdminManagedFestivalQueryApplicationService;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalCondition;
import com.example.chookjibupadmin.api.admin.dto.AdminManagedFestivalResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 개인 관리 축제 조회 API를 제공한다.
 */
@Tag(name = "Admin Managed Festival", description = "관리자 개인 관리 축제 조회 API")
@RestController
@RequestMapping("/api/admin/me/managed-festivals")
@RequiredArgsConstructor
public class AdminManagedFestivalQueryController {

    private final AdminManagedFestivalQueryApplicationService queryService;

    /**
     * 인증 관리자가 현재 관리 중인 축제 목록을 조회한다.
     */
    @Operation(summary = "내 관리 축제 목록 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ApiResponse<List<AdminManagedFestivalResponse>> getManagedFestivals(
            @RequestParam(required = false) AdminRole role,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.ADMIN_MANAGED_FESTIVAL_READ_SUCCESS,
                queryService.searchManagedFestivals(
                                new AdminManagedFestivalCondition(
                                        role,
                                        year,
                                        keyword
                                ),
                                principal
                        )
                        .stream()
                        .map(AdminManagedFestivalResponse::from)
                        .toList()
        );
    }

    /**
     * 인증 관리자가 현재 관리 중인 축제를 단건 조회한다.
     */
    @Operation(summary = "내 관리 축제 단건 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{festivalId}")
    public ApiResponse<AdminManagedFestivalResponse> getManagedFestival(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.ADMIN_MANAGED_FESTIVAL_READ_SUCCESS,
                AdminManagedFestivalResponse.from(
                        queryService.getManagedFestival(
                                festivalId,
                                principal
                        )
                )
        );
    }
}
