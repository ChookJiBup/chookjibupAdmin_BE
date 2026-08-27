package com.example.chookjibupadmin.api.booth;

import com.example.chookjibupadmin.api.booth.dto.BoothCongestionResponse;
import com.example.chookjibupadmin.api.booth.dto.UpdateBoothCongestionRequest;
import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.booth.command.application.BoothCongestionCommandApplicationService;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 승인된 부스의 혼잡 이력을 저장하는 API이다.
 */
@Tag(name = "Festival Booth", description = "축제 부스 승인·혼잡 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/booths/{boothId}/congestion")
@RequiredArgsConstructor
public class BoothCongestionCommandController {

    private final BoothCongestionCommandApplicationService congestionCommandApplicationService;

    @Operation(summary = "부스 혼잡 이력 저장")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping
    public ApiResponse<BoothCongestionResponse> updateCongestion(
            @PathVariable UUID festivalId,
            @PathVariable Long boothId,
            @Valid @RequestBody UpdateBoothCongestionRequest request,
            @AuthenticationPrincipal Object principal
    ) {
        return ApiResponse.success(
                SuccessCode.BOOTH_CONGESTION_UPDATE_SUCCESS,
                BoothCongestionResponse.from(
                        congestionCommandApplicationService.record(
                                festivalId,
                                boothId,
                                request.toCommand(),
                                requireActor(principal)
                        )
                )
        );
    }

    private FestivalActorPrincipal requireActor(Object principal) {
        if (principal instanceof FestivalActorPrincipal actor) {
            return actor;
        }
        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }
}
