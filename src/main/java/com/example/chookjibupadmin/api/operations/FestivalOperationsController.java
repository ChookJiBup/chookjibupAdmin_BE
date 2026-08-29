package com.example.chookjibupadmin.api.operations;

import com.example.chookjibupadmin.api.operations.dto.FestivalCongestionResponse;
import com.example.chookjibupadmin.api.operations.dto.FestivalOperationSuggestionResponse;
import com.example.chookjibupadmin.api.operations.dto.FestivalQueueListResponse;
import com.example.chookjibupadmin.api.operations.dto.FestivalQueueResponse;
import com.example.chookjibupadmin.api.operations.dto.UpdateFestivalQueueRequest;
import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.booth.command.application.BoothQueueCommandApplicationService;
import com.example.chookjibupadmin.booth.query.application.BoothCongestionQueryApplicationService;
import com.example.chookjibupadmin.booth.query.application.BoothQueueQueryApplicationService;
import com.example.chookjibupadmin.booth.query.application.FestivalOperationSuggestionQueryApplicationService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 축제 현장 운영(혼잡·대기열·제안) API를 제공한다.
 */
@Tag(name = "Festival Operations", description = "축제 현장 운영 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/operations")
@RequiredArgsConstructor
public class FestivalOperationsController {

    private final BoothCongestionQueryApplicationService congestionQueryApplicationService;
    private final BoothQueueQueryApplicationService queueQueryApplicationService;
    private final BoothQueueCommandApplicationService queueCommandApplicationService;
    private final FestivalOperationSuggestionQueryApplicationService suggestionQueryApplicationService;

    @Operation(summary = "전체·부스별 혼잡도 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/congestion")
    public ApiResponse<FestivalCongestionResponse> getCongestion(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal Object principal
    ) {
        FestivalActorPrincipal actor = requireActor(principal);
        return ApiResponse.success(
                SuccessCode.FESTIVAL_CONGESTION_READ_SUCCESS,
                FestivalCongestionResponse.from(
                        festivalId,
                        congestionQueryApplicationService.getCongestion(festivalId, actor)
                )
        );
    }

    @Operation(summary = "부스별 대기열·줄끝 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/queues")
    public ApiResponse<FestivalQueueListResponse> getQueues(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal Object principal
    ) {
        FestivalActorPrincipal actor = requireActor(principal);
        return ApiResponse.success(
                SuccessCode.FESTIVAL_QUEUE_READ_SUCCESS,
                FestivalQueueListResponse.from(
                        festivalId,
                        queueQueryApplicationService.getQueues(festivalId, actor)
                )
        );
    }

    @Operation(summary = "대기열 줄끝 위치 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/queues/{queueId}")
    public ApiResponse<FestivalQueueResponse> updateQueue(
            @PathVariable UUID festivalId,
            @PathVariable UUID queueId,
            @Valid @RequestBody UpdateFestivalQueueRequest request,
            @AuthenticationPrincipal Object principal
    ) {
        FestivalActorPrincipal actor = requireActor(principal);
        return ApiResponse.success(
                SuccessCode.FESTIVAL_QUEUE_UPDATE_SUCCESS,
                FestivalQueueResponse.from(
                        queueCommandApplicationService.updateTail(
                                festivalId,
                                queueId,
                                request.toCommand(),
                                actor
                        )
                )
        );
    }

    @Operation(summary = "AI/규칙 기반 운영 제안 목록")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/suggestions")
    public ApiResponse<FestivalOperationSuggestionResponse> getSuggestions(
            @PathVariable UUID festivalId,
            @AuthenticationPrincipal Object principal
    ) {
        FestivalActorPrincipal actor = requireActor(principal);
        return ApiResponse.success(
                SuccessCode.FESTIVAL_OPERATION_SUGGESTION_READ_SUCCESS,
                FestivalOperationSuggestionResponse.from(
                        suggestionQueryApplicationService.getSuggestions(festivalId, actor)
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
