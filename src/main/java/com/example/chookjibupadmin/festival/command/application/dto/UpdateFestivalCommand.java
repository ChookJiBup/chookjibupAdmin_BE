package com.example.chookjibupadmin.festival.command.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateFestivalCommand(
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
