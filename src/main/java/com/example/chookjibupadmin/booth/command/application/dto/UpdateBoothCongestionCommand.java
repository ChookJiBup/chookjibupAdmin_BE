package com.example.chookjibupadmin.booth.command.application.dto;

import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;

public record UpdateBoothCongestionCommand(
        int waitMinutes,
        BoothCongestionLevel congestionLevel
) {
}
