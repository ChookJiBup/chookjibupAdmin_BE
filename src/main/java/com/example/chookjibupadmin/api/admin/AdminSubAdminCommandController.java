package com.example.chookjibupadmin.api.admin;

import com.example.chookjibupadmin.admin.command.application.AdminSubAdminDeleteService;
import com.example.chookjibupadmin.api.admin.dto.DeleteSubAdminsRequest;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 제1 관리자 권한의 제2관리자 변경 API를 제공한다.
 */
@Tag(name = "Admin Sub Admin", description = "제1 관리자용 제2관리자 관리 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/sub-admins")
@RequiredArgsConstructor
public class AdminSubAdminCommandController {

    private final AdminSubAdminDeleteService adminSubAdminDeleteService;

    /**
     * 선택한 제2관리자의 해당 축제 권한을 일괄 삭제한다.
     */
    @Operation(summary = "제2관리자 일괄 삭제")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping
    public ApiResponse<Void> deleteAll(
            @PathVariable UUID festivalId,
            @Valid @RequestBody DeleteSubAdminsRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        adminSubAdminDeleteService.deleteAll(
                festivalId,
                request.adminIds(),
                principal
        );
        return ApiResponse.success(SuccessCode.ADMIN_SUB_ADMIN_BULK_DELETE_SUCCESS);
    }
}
