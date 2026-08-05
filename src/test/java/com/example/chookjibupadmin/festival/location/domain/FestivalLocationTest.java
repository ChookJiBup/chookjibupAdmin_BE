package com.example.chookjibupadmin.festival.location.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.global.response.CustomException;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FestivalLocationTest {
    private final Festival festival = mock(Festival.class);

    @Test
    void createsLocationWithCoordinatesAndAddress() {
        FestivalLocation location =
                FestivalLocation.create(
                        festival,
                        FestivalLocationType.MAIN_VENUE,
                        "중앙 광장",
                        " 서울특별시 마포구 월드컵로 243 ",
                        null,
                        "광장",
                        null,
                        null,
                        new BigDecimal("37.1234567"),
                        new BigDecimal("127.1234567"),
                        true,
                        0,
                        2L
                );
        assertThat(location.getRoadAddress()).isEqualTo("서울특별시 마포구 월드컵로 243");
        assertThat(location.isPrimary()).isTrue();
    }

    @Test
    void rejectsOnlyOneCoordinate() {
        assertThatThrownBy(
                () ->
                        FestivalLocation.create(
                                festival,
                                FestivalLocationType.PARKING,
                                "주차장",
                                null,
                                null,
                                null,
                                null,
                                null,
                                new BigDecimal("37.1"),
                                null,
                                false,
                                1,
                                2L
                        ))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void rejectsLocationWithoutAddressOrCoordinates() {
        assertThatThrownBy(
                () ->
                        FestivalLocation.create(
                                festival,
                                FestivalLocationType.OPERATING_AREA,
                                "운영 구역",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false,
                                1,
                                2L
                        ))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void createsBoundaryOnlyLocationAsManualSource() {
        FestivalLocation location =
                FestivalLocation.create(
                        festival,
                        FestivalLocationType.OPERATING_AREA,
                        "한강 운영 권역",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of("type", "Polygon", "coordinates", java.util.List.of()),
                        true,
                        0,
                        2L
                );

        assertThat(location.getBoundaryGeometry()).containsEntry("type", "Polygon");
        assertThat(location.getSourceType()).isEqualTo(FestivalLocationSourceType.MANUAL);
    }
}
