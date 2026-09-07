package com.example.chookjibupadmin.api.festival.dto;

import jakarta.validation.Validation;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateFestivalRequestTest {

    @Test
    @DisplayName("운영 시간을 생략한 수정 요청은 검증을 통과하고 null Command로 전달한다")
    void success_ToCommand_OmittedOperationTime() {
        // given
        UpdateFestivalRequest request = new UpdateFestivalRequest(
                "광주비엔날레", "수정 축제 설명",
                "광주광역시 북구 비엔날레로 111", "광주비엔날레 전시관",
                LocalDate.of(2026, 9, 5), LocalDate.of(2026, 11, 15),
                null, null
        );

        // when & then
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request)).isEmpty();
        }
        assertThat(request.toCommand().operationStartTime()).isNull();
        assertThat(request.toCommand().operationEndTime()).isNull();
    }


    @Test
    @DisplayName("축제 수정 요청을 상세주소가 포함된 Command로 변환한다")
    void success_ToCommand() {
        // given
        UpdateFestivalRequest request = new UpdateFestivalRequest(
                "광주비엔날레",
                "수정 축제 설명",
                "광주광역시 북구 비엔날레로 111",
                "광주비엔날레 전시관",
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 11, 15),
                LocalTime.of(10, 0),
                LocalTime.of(18, 0)
        );

        // when
        var command = request.toCommand();

        // then
        assertThat(command.address()).isEqualTo(request.address());
        assertThat(command.detailAddress()).isEqualTo(request.detailAddress());
    }
}
