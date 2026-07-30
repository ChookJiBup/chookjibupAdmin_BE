package com.example.demoadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.domain.FestivalMapRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalMapServiceTest {

    @InjectMocks
    private FestivalMapService festivalMapService;

    @Mock
    private FestivalMapRepository festivalMapRepository;

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("배치도가 없으면 예외를 던진다")
        void fail_GetById_CustomException() {
            // given
            Long festivalMapId = 1L;
            given(festivalMapRepository.findById(festivalMapId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> festivalMapService.getById(festivalMapId))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.FESTIVAL_MAP_NOT_FOUND.getMessage());
        }
    }
}
