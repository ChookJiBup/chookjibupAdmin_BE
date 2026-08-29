package com.example.chookjibupadmin.booth.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.booth.command.application.BoothCongestionService;
import com.example.chookjibupadmin.booth.command.application.BoothInfoService;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.booth.query.application.dto.FestivalOperationSuggestionView;
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalOperationSuggestionQueryApplicationServiceTest {

    @InjectMocks
    private FestivalOperationSuggestionQueryApplicationService service;

    @Mock
    private FestivalOperationAccessService festivalOperationAccessService;

    @Mock
    private BoothInfoService boothInfoService;

    @Mock
    private BoothCongestionService boothCongestionService;

    @Test
    @DisplayName("HIGH 혼잡 부스에 대한 규칙 제안을 만든다")
    void success_GetSuggestions_HighCongestion() {
        UUID festivalPublicId = UUID.randomUUID();
        AdminPrincipal principal = new AdminPrincipal(1L, "a@mapo.go.kr");
        BoothInfo booth = BoothInfo.create(10L, 100L, "김밥부스");
        ReflectionTestUtils.setField(booth, "id", 7L);
        given(festivalOperationAccessService.getAuthorizedFestivalId(festivalPublicId, principal))
                .willReturn(10L);
        given(boothInfoService.findAllByFestivalId(10L)).willReturn(List.of(booth));
        given(boothCongestionService.findLatestByBoothIds(List.of(7L)))
                .willReturn(List.of(BoothCongestion.recordByAdmin(
                        7L, 1L, 5, BoothCongestionLevel.HIGH
                )));

        FestivalOperationSuggestionView view = service.getSuggestions(festivalPublicId, principal);

        assertThat(view.suggestions()).hasSize(1);
        assertThat(view.suggestions().getFirst().suggestionId()).isEqualTo("rule-high-7");
        assertThat(view.suggestions().getFirst().title()).contains("김밥부스");
    }
}
