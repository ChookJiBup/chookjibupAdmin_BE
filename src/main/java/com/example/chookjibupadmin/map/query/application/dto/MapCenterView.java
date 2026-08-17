package com.example.chookjibupadmin.map.query.application.dto;

import java.math.BigDecimal;

/** 카카오맵 초기 중심 좌표이다. */
public record MapCenterView(
        BigDecimal lat,
        BigDecimal lng
) {
}
