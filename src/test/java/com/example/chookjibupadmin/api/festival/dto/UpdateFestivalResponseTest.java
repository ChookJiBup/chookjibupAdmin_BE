package com.example.chookjibupadmin.api.festival.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateFestivalResponseTest {

    @Test
    @DisplayName("축제를 상세주소가 포함된 수정 응답으로 변환한다")
    void success_From() {
        // given
        Festival festival = festival();

        // when
        UpdateFestivalResponse response = UpdateFestivalResponse.from(festival);

        // then
        assertThat(response.address()).isEqualTo(festival.getAddressValue());
        assertThat(response.detailAddress())
                .isEqualTo(festival.getDetailAddressValue());
    }

    private Festival festival() {
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("광주비엔날레"),
                FestivalDescription.of("축제 설명"),
                FestivalAddress.of("광주광역시 북구 비엔날레로 111"),
                FestivalDetailAddress.of("광주비엔날레 전시관"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 9, 5),
                        LocalDate.of(2026, 11, 15)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(18, 0)
                )
        );
    }
}
