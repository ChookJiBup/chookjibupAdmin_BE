package com.example.chookjibupadmin.api.fieldstaff;

import com.example.chookjibupadmin.api.fieldstaff.dto.CreateFieldStaffRequest;
import com.example.chookjibupadmin.api.fieldstaff.dto.CreateFieldStaffResponse;
import com.example.chookjibupadmin.api.fieldstaff.dto.DeleteFieldStaffRequest;
import com.example.chookjibupadmin.api.fieldstaff.dto.ReissueFieldStaffPasswordResponse;
import com.example.chookjibupadmin.api.fieldstaff.dto.UpdateFieldStaffRequest;
import com.example.chookjibupadmin.api.fieldstaff.dto.UpdateFieldStaffStatusRequest;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import com.example.chookjibupadmin.operator.command.application.FieldStaffManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 권한의 현장 스태프 계정 쓰기 API를 제공한다.
 */
@Tag(name = "Field Staff", description = "현장 스태프 계정 관리 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/field-staff")
@RequiredArgsConstructor
public class FieldStaffCommandController {

    private final FieldStaffManagementService fieldStaffManagementService;

    /**
     * 1관리자 또는 2관리자 권한으로 현장 스태프 계정을 생성한다.
     */
    @Operation(summary = "현장 스태프 계정 생성")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<CreateFieldStaffResponse> create(
            @PathVariable UUID festivalId,
            @Valid @RequestBody CreateFieldStaffRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FIELD_STAFF_CREATE_SUCCESS,
                CreateFieldStaffResponse.from(
                        fieldStaffManagementService.create(
                                festivalId,
                                request.toCommand(),
                                principal
                        )
                )
        );
    }

    /**
     * 1관리자 또는 2관리자 권한으로 여러 현장 스태프 계정을 일괄 삭제한다.
     */
    @Operation(summary = "현장 스태프 계정 일괄 삭제")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping
    public ApiResponse<Void> deleteAll(
            @PathVariable UUID festivalId,
            @Valid @RequestBody DeleteFieldStaffRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        fieldStaffManagementService.deleteAll(
                festivalId,
                request.staffIds(),
                principal
        );

        return ApiResponse.success(SuccessCode.FIELD_STAFF_BULK_DELETE_SUCCESS);
    }

    /**
     * 1관리자 또는 2관리자 권한으로 현장 스태프 계정을 삭제한다.
     */
    @Operation(summary = "현장 스태프 계정 삭제")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{staffId}")
    public ApiResponse<Void> delete(
            @PathVariable UUID festivalId,
            @PathVariable UUID staffId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        fieldStaffManagementService.delete(
                festivalId,
                staffId,
                principal
        );

        return ApiResponse.success(SuccessCode.FIELD_STAFF_DELETE_SUCCESS);
    }

    @Operation(summary = "현장 스태프 정보 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{staffId}")
    public ApiResponse<Void> update(
            @PathVariable UUID festivalId,
            @PathVariable UUID staffId,
            @Valid @RequestBody UpdateFieldStaffRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        fieldStaffManagementService.update(
                festivalId,
                staffId,
                request.toCommand(),
                principal
        );
        return ApiResponse.success(SuccessCode.FIELD_STAFF_UPDATE_SUCCESS);
    }

    @Operation(summary = "현장 스태프 임시 비밀번호 재발급")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{staffId}/password/reissue")
    public ApiResponse<ReissueFieldStaffPasswordResponse> reissuePassword(
            @PathVariable UUID festivalId,
            @PathVariable UUID staffId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FIELD_STAFF_PASSWORD_REISSUE_SUCCESS,
                new ReissueFieldStaffPasswordResponse(
                        fieldStaffManagementService.reissuePassword(
                                festivalId,
                                staffId,
                                principal
                        )
                )
        );
    }

    @Operation(summary = "현장 스태프 활성 상태 변경")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{staffId}/status")
    public ApiResponse<Void> updateStatus(
            @PathVariable UUID festivalId,
            @PathVariable UUID staffId,
            @Valid @RequestBody UpdateFieldStaffStatusRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        fieldStaffManagementService.changeActiveStatus(
                festivalId,
                staffId,
                request.active(),
                principal
        );
        return ApiResponse.success(SuccessCode.FIELD_STAFF_STATUS_UPDATE_SUCCESS);
    }
}
