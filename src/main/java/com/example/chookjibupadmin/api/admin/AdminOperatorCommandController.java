package com.example.chookjibupadmin.api.admin;

import com.example.chookjibupadmin.admin.command.application.AdminOperatorRegistrationService;
import com.example.chookjibupadmin.api.admin.dto.RegisterOperatorRequest;
import com.example.chookjibupadmin.api.admin.dto.RegisterOperatorResponse;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 총괄 관리자의 운영자 등록 API를 제공한다.
 */
@Tag(name = "Admin Operator", description = "운영자 등록 API")
@RestController
@RequestMapping("/api/festivals/{festivalId}/operators")
@RequiredArgsConstructor
public class AdminOperatorCommandController {

    private final AdminOperatorRegistrationService operatorRegistrationService;

    @Operation(summary = "운영자 등록")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<RegisterOperatorResponse> register(
            @PathVariable UUID festivalId,
            @Valid @RequestBody RegisterOperatorRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.success(
                SuccessCode.ADMIN_OPERATOR_REGISTER_SUCCESS,
                RegisterOperatorResponse.from(operatorRegistrationService.register(
                        festivalId,
                        request.email(),
                        request.name(),
                        request.companyName(),
                        principal
                ))
        );
    }
}
