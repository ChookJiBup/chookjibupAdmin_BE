package com.example.demoadmin.booth.command.application.dto;

public record CreateBoothCommand(
        String name,
        String category,
        String location,
        String description
) {
}
