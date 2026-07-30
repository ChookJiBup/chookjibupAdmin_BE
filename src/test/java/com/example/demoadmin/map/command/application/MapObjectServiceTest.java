package com.example.demoadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.demoadmin.map.command.domain.MapObjectRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapObjectServiceTest {

    @InjectMocks
    private MapObjectService mapObjectService;

    @Mock
    private MapObjectRepository mapObjectRepository;

    @Nested
    @DisplayName("findByFestivalMapId")
    class FindByFestivalMapId {

        @Test
        @DisplayName("배치도 ID로 객체 목록을 조회한다")
        void success_FindByFestivalMapId() {
            // given
            Long festivalMapId = 1L;
            given(mapObjectRepository.findByFestivalMapId(festivalMapId))
                    .willReturn(List.of());

            // when
            var result = mapObjectService.findByFestivalMapId(festivalMapId);

            // then
            assertThat(result).isEmpty();
        }
    }
}
