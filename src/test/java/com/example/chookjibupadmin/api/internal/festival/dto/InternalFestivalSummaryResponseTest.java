package com.example.chookjibupadmin.api.internal.festival.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalProgressStatus;
import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSummaryView;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InternalFestivalSummaryResponseTest {

    @Test
    @DisplayName("축제 요약을 상세주소가 포함된 사용자 서버 응답으로 변환한다")
    void success_From() {
        // given
        InternalFestivalSummaryView view = new InternalFestivalSummaryView(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "광주비엔날레",
                "축제 설명",
                "광주광역시 북구 비엔날레로 111",
                "광주비엔날레 전시관",
                2026,
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 11, 15),
                LocalTime.of(10, 0),
                LocalTime.of(18, 0),
                InternalFestivalProgressStatus.UPCOMING
        );

        // when
        InternalFestivalSummaryResponse response =
                InternalFestivalSummaryResponse.from(view);

        // then
        assertThat(response.address()).isEqualTo(view.address());
        assertThat(response.detailAddress()).isEqualTo(view.detailAddress());
    }
}
