package com.example.demoadmin.booth.command.application.dto;

public record UpdateBoothCommand(
        String name,
        String category,
        String location,
        String description
) {
}
