package com.example.chookjibupadmin.api.admin;

import com.example.chookjibupadmin.admin.query.application.AdminSubAdminQueryApplicationService;
import com.example.chookjibupadmin.api.admin.dto.AdminSubAdminResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 제1 관리자 권한의 서브관리자 조회 API를 제공한다.
 */
@Tag(name = "Admin Sub Admin", description = "제1 관리자용 서브관리자 조회 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/sub-admins")
@RequiredArgsConstructor
public class AdminSubAdminQueryController {

    private final AdminSubAdminQueryApplicationService subAdminQueryService;

    /**
     * 담당 축제에 등록된 서브관리자 목록을 조회한다.
     */
    @Operation(summary = "서브관리자 전체 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ApiResponse<List<AdminSubAdminResponse>> getSubAdmins(
            @PathVariable UUID festivalId,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.ADMIN_SUB_ADMIN_READ_SUCCESS,
                subAdminQueryService.getSubAdmins(festivalId, keyword, principal)
                        .stream()
                        .map(AdminSubAdminResponse::from)
                        .toList()
        );
    }

    /**
     * 담당 축제에 등록된 서브관리자를 외부 UUID로 조회한다.
     */
    @Operation(summary = "서브관리자 단건 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{adminId}")
    public ApiResponse<AdminSubAdminResponse> getSubAdmin(
            @PathVariable UUID festivalId,
            @PathVariable UUID adminId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.ADMIN_SUB_ADMIN_READ_SUCCESS,
                AdminSubAdminResponse.from(
                        subAdminQueryService.getSubAdmin(
                                festivalId,
                                adminId,
                                principal
                        )
                )
        );
    }
}
