package com.example.demoadmin.api.booth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.booth.command.application.dto.BoothQueueTailResult;
import com.example.demoadmin.booth.command.domain.BoothQueueLine;
import com.example.demoadmin.booth.command.domain.FestivalBooth;
import com.example.demoadmin.booth.command.domain.vo.BoothCategory;
import com.example.demoadmin.booth.command.domain.vo.BoothDescription;
import com.example.demoadmin.booth.command.domain.vo.BoothLineLabel;
import com.example.demoadmin.booth.command.domain.vo.BoothLocation;
import com.example.demoadmin.booth.command.domain.vo.BoothName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BoothResponseTest {

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("줄 끝 갱신 결과의 외부 라인 ID와 정보를 반환한다")
        void success_From_QueueTailResult() {
            // given
            FestivalBooth booth = booth();
            BoothQueueLine queueLine = queueLine();
            ReflectionTestUtils.setField(booth, "id", 10L);
            ReflectionTestUtils.setField(queueLine, "id", 20L);
            booth.updateQueueTail(queueLine);

            // when
            BoothResponse response = BoothResponse.from(
                    new BoothQueueTailResult(booth, queueLine)
            );

            // then
            assertThat(response.currentQueueLineId())
                    .isEqualTo(queueLine.getPublicId());
            assertThat(response.currentQueueLineOrder()).isEqualTo(1);
            assertThat(response.currentQueueLineLabel()).isEqualTo("라인 1");
            assertThat(response.expectedWaitingMinutes()).isEqualTo(10);
        }
    }

    private FestivalBooth booth() {
        return FestivalBooth.create(
                1L,
                BoothName.of("푸드 부스"),
                BoothCategory.of("먹거리"),
                BoothLocation.of("A-1"),
                BoothDescription.of("설명")
        );
    }

    private BoothQueueLine queueLine() {
        return BoothQueueLine.create(
                10L,
                1,
                BoothLineLabel.of("라인 1"),
                10,
                30,
                "{}",
                "{}"
        );
    }
}
