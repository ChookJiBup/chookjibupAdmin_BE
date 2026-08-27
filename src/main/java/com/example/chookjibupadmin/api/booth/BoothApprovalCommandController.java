package com.example.chookjibupadmin.api.booth;

import com.example.chookjibupadmin.api.booth.dto.ApproveBoothResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.booth.command.application.BoothApprovalApplicationService;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지도 노드를 승인 부스로 전환하는 API이다.
 */
@Tag(name = "Festival Booth", description = "축제 부스 승인·혼잡 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/maps/{mapId}/nodes/{nodeId}")
@RequiredArgsConstructor
public class BoothApprovalCommandController {

    private final BoothApprovalApplicationService boothApprovalApplicationService;

    @Operation(summary = "지도 부스 노드 승인")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/approve-booth")
    public ApiResponse<ApproveBoothResponse> approveBooth(
            @PathVariable UUID festivalId,
            @PathVariable UUID mapId,
            @PathVariable UUID nodeId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.BOOTH_APPROVE_SUCCESS,
                ApproveBoothResponse.from(
                        boothApprovalApplicationService.approve(
                                festivalId,
                                mapId,
                                nodeId,
                                principal
                        )
                )
        );
    }
}
