package com.example.chookjibupadmin.booth.command.domain;

/**
 * 줄끝 거리로 계산한 부스 혼잡도 추정 결과이다.
 */
public record BoothCongestionEstimate(
        BoothCongestionLevel congestionLevel,
        int waitMinutes
) {
}
