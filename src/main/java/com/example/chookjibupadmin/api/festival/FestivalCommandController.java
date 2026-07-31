package com.example.chookjibupadmin.api.festival;

import com.example.chookjibupadmin.api.festival.dto.CreateFestivalRequest;
import com.example.chookjibupadmin.api.festival.dto.CreateFestivalResponse;
import com.example.chookjibupadmin.api.festival.dto.UpdateFestivalRequest;
import com.example.chookjibupadmin.api.festival.dto.UpdateFestivalResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalApplicationService;
import com.example.chookjibupadmin.global.response.ApiResponse;
import com.example.chookjibupadmin.global.response.SuccessCode;
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

/**
 * 축제 기본 정보 쓰기 API를 제공한다.
 */
@Tag(name = "Festival", description = "축제 기본 정보 API")
@RestController
@RequestMapping("/api/festivals")
@RequiredArgsConstructor
public class FestivalCommandController {

    private final FestivalApplicationService festivalApplicationService;

    /**
     * 임시 기준의 축제 기본 정보를 저장한다.
     */
    @Operation(summary = "축제 기본 정보 생성")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<CreateFestivalResponse> create(
            @Valid @RequestBody CreateFestivalRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_CREATE_SUCCESS,
                CreateFestivalResponse.from(
                        festivalApplicationService.create(
                                request.toCommand(),
                                principal
                        )
                )
        );
    }

    /**
     * 1관리자 권한으로 담당 축제의 기본 정보를 수정한다.
     */
    @Operation(summary = "축제 기본 정보 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{festivalId}")
    public ApiResponse<UpdateFestivalResponse> update(
            @PathVariable UUID festivalId,
            @Valid @RequestBody UpdateFestivalRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.FESTIVAL_UPDATE_SUCCESS,
                UpdateFestivalResponse.from(
                        festivalApplicationService.update(
                                festivalId,
                                request.toCommand(),
                                principal
                        )
                )
        );
    }
}
