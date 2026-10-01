package com.example.chookjibupadmin.festival.support;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 비공개 저장소의 대표 이미지를 읽는 공개 API 주소를 만든다. */
@Component
public class FestivalPublicImageUrlBuilder {

    private final String baseUrl;

    public FestivalPublicImageUrlBuilder(
            @Value("${app.festival.public-image-base-url:http://localhost:8080}") String baseUrl
    ) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public String build(UUID festivalId) {
        return "%s/api/public/festivals/%s/image".formatted(baseUrl, festivalId);
    }
}
