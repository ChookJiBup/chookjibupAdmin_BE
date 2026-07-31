package com.example.chookjibupadmin.festival.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.festival.command.domain.FestivalSeries;
import com.example.chookjibupadmin.festival.command.domain.FestivalSeriesRepository;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalSeriesServiceTest {

    @InjectMocks
    private FestivalSeriesService festivalSeriesService;

    @Mock
    private FestivalSeriesRepository festivalSeriesRepository;

    @Nested
    @DisplayName("getByPublicId")
    class GetByPublicId {

        @Test
        @DisplayName("외부 UUID로 축제 묶음을 조회한다")
        void success_GetByPublicId() {
            // given
            FestivalSeries series = festivalSeries();
            given(festivalSeriesRepository.findByPublicId(series.getPublicId()))
                    .willReturn(Optional.of(series));

            // when
            FestivalSeries found = festivalSeriesService.getByPublicId(
                    series.getPublicId()
            );

            // then
            assertThat(found).isSameAs(series);
        }

        @Test
        @DisplayName("축제 묶음이 없으면 예외를 던진다")
        void fail_GetByPublicId_CustomException() {
            // given
            UUID publicId = UUID.randomUUID();
            given(festivalSeriesRepository.findByPublicId(publicId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> festivalSeriesService.getByPublicId(publicId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_SERIES_NOT_FOUND.getMessage());
        }
    }

    private FestivalSeries festivalSeries() {
        return FestivalSeries.create(FestivalName.of("마포나루 새우젓축제"));
    }
}
