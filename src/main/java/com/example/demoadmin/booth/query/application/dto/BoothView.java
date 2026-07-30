package com.example.demoadmin.booth.query.application.dto;

import com.example.demoadmin.booth.command.domain.BoothOperatingStatus;
import java.util.UUID;

public record BoothView(
        UUID boothId,
        String name,
        String category,
        String location,
        String description,
        BoothOperatingStatus operatingStatus,
        UUID currentQueueLineId,
        Integer currentQueueLineOrder,
        String currentQueueLineLabel,
        int expectedWaitingMinutes
) {
}
