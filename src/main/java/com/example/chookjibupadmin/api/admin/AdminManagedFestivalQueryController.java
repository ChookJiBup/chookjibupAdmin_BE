package com.example.chookjibupadmin.api.admin;

import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.query.application.AdminManagedFestivalDetailQueryApplicationService;
import com.example.chookjibupadmin.admin.query.application.AdminManagedFestivalQueryApplicationService;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalCondition;
import com.example.chookjibupadmin.api.admin.dto.AdminManagedFestivalDetailResponse;
import com.example.chookjibupadmin.api.admin.dto.AdminManagedFestivalResponse;
import com.example.chookjibupadmin.api.admin.dto.ReviewQrResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
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
    private final AdminManagedFestivalDetailQueryApplicationService detailQueryService;

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
            @RequestParam(required = false) FestivalProgressStatus progressStatus,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.ADMIN_MANAGED_FESTIVAL_READ_SUCCESS,
                queryService.searchManagedFestivals(
                                new AdminManagedFestivalCondition(
                                        role,
                                        year,
                                        keyword,
                                        progressStatus
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
    public ApiResponse<AdminManagedFestivalDetailResponse> getManagedFestival(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.ADMIN_MANAGED_FESTIVAL_READ_SUCCESS,
                AdminManagedFestivalDetailResponse.from(
                        detailQueryService.getManagedFestival(
                                festivalId,
                                principal
                        )
                )
        );
    }

    /**
     * 축제 현장에 붙일 리뷰 QR코드를 조회한다. URL과 함께, 바로 화면에 그릴 수 있는
     * PNG 이미지(base64 data URI)도 같이 내려준다. getManagedFestival과 똑같이
     * 본인이 관리하는 축제인지 검증한다.
     */
    @Operation(
            summary = "축제 현장 리뷰 QR 조회",
            description = "리뷰 작성 URL과 그 URL을 인코딩한 PNG QR코드 이미지(base64 data URI)를 함께 반환한다. "
                    + "size는 QR 이미지의 정사각형 한 변 픽셀 크기(기본 480, 생략 가능)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{festivalId}/review-qr")
    public ApiResponse<ReviewQrResponse> getReviewQr(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_REVIEW_QR_READ_SUCCESS,
                ReviewQrResponse.from(
                        detailQueryService.getReviewQr(festivalId, principal)
                )
        );
    }
}