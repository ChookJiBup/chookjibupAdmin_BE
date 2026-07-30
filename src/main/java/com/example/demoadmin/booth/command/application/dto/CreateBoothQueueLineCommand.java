package com.example.demoadmin.booth.command.application.dto;

public record CreateBoothQueueLineCommand(
        int lineOrder,
        String label,
        int expectedWaitingMinutes,
        int maxCapacity,
        String pathData,
        String entryPointData
) {
}
