package com.example.chookjibupadmin.festival.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class FestivalPublicImageUrlBuilderTest {

    @Test
    void buildsStablePublicImageEndpoint() {
        UUID festivalId = UUID.randomUUID();
        FestivalPublicImageUrlBuilder builder =
                new FestivalPublicImageUrlBuilder("https://api.chookjibup.store/");

        assertThat(builder.build(festivalId)).isEqualTo(
                "https://api.chookjibup.store/api/public/festivals/"
                        + festivalId + "/image"
        );
    }
}
