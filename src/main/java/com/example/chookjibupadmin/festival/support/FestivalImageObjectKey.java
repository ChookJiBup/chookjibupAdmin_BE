package com.example.chookjibupadmin.festival.support;

import java.util.UUID;

/**
 * 축제 대표 이미지가 저장되는 객체 키 규칙을 제공한다.
 */
public final class FestivalImageObjectKey {

    private FestivalImageObjectKey() {
    }

    public static String representative(UUID festivalId) {
        return "public/festivals/%s/representative.png".formatted(festivalId);
    }
}
