package com.example.chookjibupadmin.map.command.application.dto;

import java.net.URI;
import java.time.Instant;

/**
 * 제한 시간 동안 배치도 display 이미지를 읽을 수 있는 URL이다.
 */
public record MapImageReadUrl(
        URI url,
        Instant expiresAt
) {
}
