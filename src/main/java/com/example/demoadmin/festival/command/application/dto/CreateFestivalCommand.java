package com.example.demoadmin.festival.command.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateFestivalCommand(
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
}
