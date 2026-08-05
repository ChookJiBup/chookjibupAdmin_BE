package com.example.chookjibupadmin.festival.command.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreateFestivalCommand(
        UUID seriesId,
        String name,
        String description,
        List<FestivalLocationCommand> locations,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime operationStartTime,
        LocalTime operationEndTime
) {
    public CreateFestivalCommand(
            UUID seriesId,
            String name,
            String description,
            String address,
            String detailAddress,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime operationStartTime,
            LocalTime operationEndTime
    ) {
        this(
                seriesId,
                name,
                description,
                List.of(FestivalLocationCommand.legacy(address, detailAddress)),
                startDate,
                endDate,
                operationStartTime,
                operationEndTime
        );
    }

    public String address() {
        FestivalLocationCommand p = primary();
        return p.roadAddress() != null ? p.roadAddress() : p.jibunAddress();
    }

    public String detailAddress() {
        return primary().detailAddress();
    }

    private FestivalLocationCommand primary() {
        return locations.stream()
                .filter(FestivalLocationCommand::primary)
                .findFirst()
                .orElse(locations.getFirst());
    }
}
