package com.example.demoadmin.api.festival.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.festival.query.application.dto.FestivalSeriesSearchView;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FestivalSeriesSearchResponseTest {

    @Test
    @DisplayName("축제 시리즈 조회 결과를 API 응답으로 변환한다")
    void success_From() {
        // given
        FestivalSeriesSearchView view = new FestivalSeriesSearchView(
                UUID.randomUUID(),
                "김밥축제",
                UUID.randomUUID(),
                2025,
                "김밥 축제 설명",
                "경상북도 김천시",
                "김천종합스포츠타운",
                LocalDate.of(2025, 10, 1),
                LocalDate.of(2025, 10, 3),
                LocalTime.of(10, 0),
                LocalTime.of(20, 0)
        );

        // when
        FestivalSeriesSearchResponse response =
                FestivalSeriesSearchResponse.from(view);

        // then
        assertThat(response.seriesId()).isEqualTo(view.seriesId());
        assertThat(response.latestFestivalId())
                .isEqualTo(view.latestFestivalId());
        assertThat(response.latestAddress()).isEqualTo(view.latestAddress());
        assertThat(response.latestDetailAddress())
                .isEqualTo(view.latestDetailAddress());
    }
}
