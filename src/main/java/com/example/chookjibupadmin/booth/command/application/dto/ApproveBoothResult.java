package com.example.chookjibupadmin.booth.command.application.dto;

import java.util.UUID;

public record ApproveBoothResult(
        Long boothId,
        UUID nodeId,
        String boothName
) {
}
