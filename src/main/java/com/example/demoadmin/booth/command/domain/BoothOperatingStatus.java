package com.example.demoadmin.booth.command.domain;

/**
 * 축제 부스의 운영 상태를 정의한다.
 */
public enum BoothOperatingStatus {
    PREPARING,
    OPERATING,
    SATURATED,
    CLOSED;

    public boolean isClosed() {
        return this == CLOSED;
    }
}
