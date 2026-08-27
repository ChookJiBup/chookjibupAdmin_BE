package com.example.chookjibupadmin.festival.command.application.dto;

import com.example.chookjibupadmin.festival.command.domain.FestivalVisitorCountInputMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record UpdateFestivalCommand(
        String name,
        String description,
        List<FestivalLocationCommand> locations,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime operationStartTime,
        LocalTime operationEndTime,
        FestivalVisitorCountInputMode visitorCountInputMode
) {
    public UpdateFestivalCommand(
            String name,
            String description,
            List<FestivalLocationCommand> locations,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime operationStartTime,
            LocalTime operationEndTime
    ) {
        this(
                name,
                description,
                locations,
                startDate,
                endDate,
                operationStartTime,
                operationEndTime,
                null
        );
    }

    public UpdateFestivalCommand(
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
                name,
                description,
                List.of(FestivalLocationCommand.legacy(address, detailAddress)),
                startDate,
                endDate,
                operationStartTime,
                operationEndTime,
                null
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
