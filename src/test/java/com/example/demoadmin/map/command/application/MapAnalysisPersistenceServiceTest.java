package com.example.demoadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

import com.example.demoadmin.admin.command.application.AdminFestivalRoleService;
import com.example.demoadmin.festival.command.application.FestivalService;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.application.dto.CreateTestMapAnalysisCommand;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapAnalysisPersistenceServiceTest {

    @InjectMocks
    private MapAnalysisPersistenceService mapAnalysisPersistenceService;

    @Mock
    private FestivalService festivalService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Mock
    private FestivalMapService festivalMapService;

    @Mock
    private MapAnalysisJobService mapAnalysisJobService;

    @Mock
    private MapObjectService mapObjectService;

    @Nested
    @DisplayName("prepare")
    class Prepare {

        @Test
        @DisplayName("인증 주체가 없으면 분석 작업을 생성할 수 없다")
        void fail_Prepare_CustomException_Unauthorized() {
            // given
            UUID festivalId = UUID.randomUUID();
            CreateTestMapAnalysisCommand command = command();

            // when & then
            assertThatThrownBy(() -> mapAnalysisPersistenceService.prepare(
                    festivalId,
                    command,
                    null
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());
            then(festivalService).shouldHaveNoInteractions();
        }
    }

    private CreateTestMapAnalysisCommand command() {
        return new CreateTestMapAnalysisCommand(
                "김밥축제_지적편집도.png",
                "images/김밥축제_지적편집도.png",
                1745,
                1577
        );
    }
}
