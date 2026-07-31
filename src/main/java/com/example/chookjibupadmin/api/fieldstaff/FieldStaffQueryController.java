package com.example.chookjibupadmin.api.fieldstaff;

import com.example.chookjibupadmin.api.fieldstaff.dto.FieldStaffResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import com.example.chookjibupadmin.operator.query.application.FieldStaffQueryApplicationService;
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
 * 관리자 권한의 현장 스태프 계정 조회 API를 제공한다.
 */
@Tag(name = "Field Staff", description = "현장 스태프 계정 관리 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/field-staff")
@RequiredArgsConstructor
public class FieldStaffQueryController {

    private final FieldStaffQueryApplicationService fieldStaffQueryService;

    /**
     * 1관리자 또는 2관리자 권한으로 현장 스태프 계정 목록을 조회한다.
     */
    @Operation(summary = "현장 스태프 전체 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ApiResponse<List<FieldStaffResponse>> getFieldStaff(
            @PathVariable UUID festivalId,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FIELD_STAFF_READ_SUCCESS,
                fieldStaffQueryService.searchFieldStaff(
                                festivalId,
                                keyword,
                                principal
                        )
                        .stream()
                        .map(FieldStaffResponse::from)
                        .toList()
        );
    }

    /**
     * 1관리자 또는 2관리자 권한으로 현장 스태프 계정을 단건 조회한다.
     */
    @Operation(summary = "현장 스태프 단건 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{staffId}")
    public ApiResponse<FieldStaffResponse> getFieldStaff(
            @PathVariable UUID festivalId,
            @PathVariable UUID staffId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FIELD_STAFF_READ_SUCCESS,
                FieldStaffResponse.from(
                        fieldStaffQueryService.getFieldStaff(
                                festivalId,
                                staffId,
                                principal
                        )
                )
        );
    }
}
