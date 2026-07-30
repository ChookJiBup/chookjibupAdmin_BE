package com.example.demoadmin.booth.command.application.dto;

import com.example.demoadmin.booth.command.domain.BoothQueueLine;
import com.example.demoadmin.booth.command.domain.FestivalBooth;

public record BoothQueueTailResult(
        FestivalBooth booth,
        BoothQueueLine currentQueueLine
) {
}
