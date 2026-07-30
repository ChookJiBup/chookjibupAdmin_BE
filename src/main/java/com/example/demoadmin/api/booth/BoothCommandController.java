package com.example.demoadmin.api.booth;

import com.example.demoadmin.api.booth.dto.BoothQueueLineResponse;
import com.example.demoadmin.api.booth.dto.BoothResponse;
import com.example.demoadmin.api.booth.dto.CreateBoothQueueLineRequest;
import com.example.demoadmin.api.booth.dto.CreateBoothRequest;
import com.example.demoadmin.api.booth.dto.UpdateBoothQueueLineRequest;
import com.example.demoadmin.api.booth.dto.UpdateBoothQueueTailRequest;
import com.example.demoadmin.api.booth.dto.UpdateBoothRequest;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.auth.support.FestivalAccessPrincipal;
import com.example.demoadmin.booth.command.application.BoothApplicationService;
import com.example.demoadmin.global.response.ApiResponse;
import com.example.demoadmin.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Booth", description = "축제 부스 및 대기 라인 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/booths")
@RequiredArgsConstructor
public class BoothCommandController {

    private final BoothApplicationService boothApplicationService;

    @Operation(summary = "축제 부스 생성")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<BoothResponse> createBooth(
            @PathVariable UUID festivalId,
            @Valid @RequestBody CreateBoothRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.BOOTH_CREATE_SUCCESS,
                BoothResponse.from(boothApplicationService.createBooth(
                        festivalId,
                        request.toCommand(),
                        principal
                ))
        );
    }

    @Operation(summary = "축제 부스 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{boothId}")
    public ApiResponse<BoothResponse> updateBooth(
            @PathVariable UUID festivalId,
            @PathVariable UUID boothId,
            @Valid @RequestBody UpdateBoothRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.BOOTH_UPDATE_SUCCESS,
                BoothResponse.from(boothApplicationService.updateBooth(
                        festivalId,
                        boothId,
                        request.toCommand(),
                        principal
                ))
        );
    }

    @Operation(summary = "부스 대기 라인 생성")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{boothId}/queue-lines")
    public ApiResponse<BoothQueueLineResponse> createQueueLine(
            @PathVariable UUID festivalId,
            @PathVariable UUID boothId,
            @Valid @RequestBody CreateBoothQueueLineRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.BOOTH_QUEUE_LINE_CREATE_SUCCESS,
                BoothQueueLineResponse.from(boothApplicationService.createQueueLine(
                        festivalId,
                        boothId,
                        request.toCommand(),
                        principal
                ))
        );
    }

    @Operation(summary = "부스 대기 라인 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{boothId}/queue-lines/{lineId}")
    public ApiResponse<BoothQueueLineResponse> updateQueueLine(
            @PathVariable UUID festivalId,
            @PathVariable UUID boothId,
            @PathVariable UUID lineId,
            @Valid @RequestBody UpdateBoothQueueLineRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.BOOTH_QUEUE_LINE_UPDATE_SUCCESS,
                BoothQueueLineResponse.from(boothApplicationService.updateQueueLine(
                        festivalId,
                        boothId,
                        lineId,
                        request.toCommand(),
                        principal
                ))
        );
    }

    @Operation(summary = "부스 줄 끝 갱신")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{boothId}/queue-tail")
    public ApiResponse<BoothResponse> updateQueueTail(
            @PathVariable UUID festivalId,
            @PathVariable UUID boothId,
            @Valid @RequestBody UpdateBoothQueueTailRequest request,
            @AuthenticationPrincipal FestivalAccessPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.BOOTH_QUEUE_TAIL_UPDATE_SUCCESS,
                BoothResponse.from(boothApplicationService.updateQueueTail(
                        festivalId,
                        boothId,
                        request.toCommand(),
                        principal
                ))
        );
    }
}
