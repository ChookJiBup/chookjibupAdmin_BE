package com.example.demoadmin.booth.command.application.dto;

public record UpdateBoothQueueLineCommand(
        int lineOrder,
        String label,
        int expectedWaitingMinutes,
        int maxCapacity,
        String pathData,
        String entryPointData
) {
}
