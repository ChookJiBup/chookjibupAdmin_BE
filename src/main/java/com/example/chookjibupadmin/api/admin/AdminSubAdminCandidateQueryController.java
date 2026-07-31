package com.example.chookjibupadmin.api.admin;

import com.example.chookjibupadmin.admin.query.application.AdminSubAdminCandidateQueryApplicationService;
import com.example.chookjibupadmin.api.admin.dto.AdminSubAdminCandidateResponse;
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
 * 제1 관리자 권한의 서브관리자 초대 후보 조회 API를 제공한다.
 */
@Tag(name = "Admin Sub Admin Candidate", description = "제1 관리자용 서브관리자 초대 후보 조회 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/sub-admin-candidates")
@RequiredArgsConstructor
public class AdminSubAdminCandidateQueryController {

    private final AdminSubAdminCandidateQueryApplicationService candidateQueryService;

    /**
     * 이미 회원가입했고 아직 축제에 배정되지 않은 관리자 후보를 검색한다.
     */
    @Operation(summary = "서브관리자 초대 후보 검색")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ApiResponse<List<AdminSubAdminCandidateResponse>> searchCandidates(
            @PathVariable UUID festivalId,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.ADMIN_SUB_ADMIN_CANDIDATE_READ_SUCCESS,
                candidateQueryService.searchCandidates(
                                festivalId,
                                keyword,
                                principal
                        )
                        .stream()
                        .map(AdminSubAdminCandidateResponse::from)
                        .toList()
        );
    }
}
