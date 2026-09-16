package com.example.chookjibupadmin.api.operations.dto;

import com.example.chookjibupadmin.booth.command.application.dto.SaveQueuePlanCommand;
import com.example.chookjibupadmin.api.operations.dto.UpdateFestivalQueueRequest.PathPointRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "사전 동선 저장. 실제 대기시간을 생성하지 않는다.")
public record SaveQueuePlanRequest(
        @NotNull @Size(min = 2, max = 500) List<@NotNull @Valid PathPointRequest> path,
        @DecimalMin("0.2") @DecimalMax("5") double metersPerPerson,
        @DecimalMin("0.1") @DecimalMax("100") double servedPersonsPerMinute,
        UUID sourceNodeId,
        @NotNull @PositiveOrZero Long expectedRevision,
        @NotNull @PositiveOrZero Long expectedNodeVersion) {
    public SaveQueuePlanCommand toCommand() {
        return new SaveQueuePlanCommand(path.stream().map(p -> Map.of("lat", p.lat(), "lng", p.lng())).toList(),
                metersPerPerson, servedPersonsPerMinute, sourceNodeId, expectedRevision, expectedNodeVersion);
    }
}
