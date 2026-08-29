package com.example.chookjibupadmin.booth.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.booth.command.application.BoothCongestionService;
import com.example.chookjibupadmin.booth.command.application.BoothInfoService;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.booth.query.application.dto.FestivalCongestionView;
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
class BoothCongestionQueryApplicationServiceTest {

    @InjectMocks
    private BoothCongestionQueryApplicationService service;

    @Mock
    private FestivalOperationAccessService festivalOperationAccessService;

    @Mock
    private BoothInfoService boothInfoService;

    @Mock
    private BoothCongestionService boothCongestionService;

    @Test
    @DisplayName("승인 부스와 최신 혼잡 이력을 합쳐 조회한다")
    void success_GetCongestion() {
        UUID festivalPublicId = UUID.randomUUID();
        AdminPrincipal principal = new AdminPrincipal(1L, "a@mapo.go.kr");
        BoothInfo booth = BoothInfo.create(10L, 100L, "김밥부스");
        ReflectionTestUtils.setField(booth, "id", 7L);
        BoothCongestion congestion = BoothCongestion.recordByAdmin(
                7L, 1L, 20, BoothCongestionLevel.HIGH
        );
        given(festivalOperationAccessService.getAuthorizedFestivalId(festivalPublicId, principal))
                .willReturn(10L);
        given(boothInfoService.findAllByFestivalId(10L)).willReturn(List.of(booth));
        given(boothCongestionService.findLatestByBoothIds(List.of(7L)))
                .willReturn(List.of(congestion));

        FestivalCongestionView view = service.getCongestion(festivalPublicId, principal);

        assertThat(view.booths()).hasSize(1);
        assertThat(view.booths().getFirst().boothName()).isEqualTo("김밥부스");
        assertThat(view.booths().getFirst().congestionLevel()).isEqualTo(BoothCongestionLevel.HIGH);
        assertThat(view.booths().getFirst().waitMinutes()).isEqualTo(20);
        assertThat(view.activeQueueCount()).isEqualTo(1);
        assertThat(view.averageWaitMinutes()).isEqualTo(20);
    }
}
