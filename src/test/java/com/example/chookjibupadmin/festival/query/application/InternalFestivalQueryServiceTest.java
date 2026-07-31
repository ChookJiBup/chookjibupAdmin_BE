package com.example.chookjibupadmin.festival.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.festival.query.application.dto.InternalFestivalSearchCondition;
import com.example.chookjibupadmin.festival.query.repository.InternalFestivalQueryRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class InternalFestivalQueryServiceTest {

    @Mock
    private InternalFestivalQueryRepository queryRepository;

    @Nested
    @DisplayName("searchFestivals")
    class SearchFestivals {

        @Test
        @DisplayName("사용자 서버용 축제 목록 projection을 조회한다")
        void success_SearchFestivals() {
            // given
            InternalFestivalQueryService service =
                    new InternalFestivalQueryService(queryRepository);
            InternalFestivalSearchCondition condition =
                    new InternalFestivalSearchCondition(
                            null,
                            null,
                            LocalDate.of(2026, 10, 9)
                    );
            PageRequest pageable = PageRequest.of(0, 20);
            given(queryRepository.searchFestivals(condition, pageable))
                    .willReturn(new PageImpl<>(List.of()));

            // when
            var result = service.searchFestivals(condition, pageable);

            // then
            assertThat(result).isEmpty();
            then(queryRepository).should().searchFestivals(condition, pageable);
        }
    }
}
