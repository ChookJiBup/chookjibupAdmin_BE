package com.example.demoadmin.booth.command.application.dto;

import java.util.UUID;

public record UpdateBoothQueueTailCommand(
        UUID queueLineId,
        String status
) {
}
