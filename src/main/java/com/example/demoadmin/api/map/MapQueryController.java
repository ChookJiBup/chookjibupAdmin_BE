package com.example.demoadmin.api.map;

import com.example.demoadmin.api.map.dto.FestivalMapObjectsResponse;
import com.example.demoadmin.auth.support.AdminPrincipal;
import com.example.demoadmin.global.response.ApiResponse;
import com.example.demoadmin.global.response.SuccessCode;
import com.example.demoadmin.map.query.application.MapQueryApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Map", description = "축제 배치도 분석 및 객체 조회 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/maps")
@RequiredArgsConstructor
public class MapQueryController {

    private final MapQueryApplicationService mapQueryApplicationService;

    @Operation(summary = "React-Konva용 배치도 객체 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{mapId}/objects")
    public ApiResponse<FestivalMapObjectsResponse> getMapObjects(
            @PathVariable UUID festivalId,
            @PathVariable UUID mapId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.MAP_OBJECT_READ_SUCCESS,
                FestivalMapObjectsResponse.from(
                        mapQueryApplicationService.getMapObjects(
                                festivalId,
                                mapId,
                                principal
                        )
                )
        );
    }
}
