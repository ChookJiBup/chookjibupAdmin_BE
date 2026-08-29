package com.example.chookjibupadmin.booth.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.booth.command.application.BoothInfoService;
import com.example.chookjibupadmin.booth.command.application.BoothQueueService;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.booth.command.domain.BoothQueue;
import com.example.chookjibupadmin.booth.query.application.dto.FestivalQueueListView;
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
class BoothQueueQueryApplicationServiceTest {

    @InjectMocks
    private BoothQueueQueryApplicationService service;

    @Mock
    private FestivalOperationAccessService festivalOperationAccessService;

    @Mock
    private BoothInfoService boothInfoService;

    @Mock
    private BoothQueueService boothQueueService;

    @Test
    @DisplayName("대기열이 없으면 승인 부스마다 빈 대기열을 만든다")
    void success_GetQueues_CreatesMissing() {
        UUID festivalPublicId = UUID.randomUUID();
        AdminPrincipal principal = new AdminPrincipal(1L, "a@mapo.go.kr");
        BoothInfo booth = BoothInfo.create(10L, 100L, "김밥부스");
        ReflectionTestUtils.setField(booth, "id", 7L);
        BoothQueue created = BoothQueue.createEmpty(10L, 7L);
        given(festivalOperationAccessService.getAuthorizedFestivalId(festivalPublicId, principal))
                .willReturn(10L);
        given(boothInfoService.findAllByFestivalId(10L)).willReturn(List.of(booth));
        given(boothQueueService.findAllByBoothIdIn(List.of(7L))).willReturn(List.of());
        given(boothQueueService.save(any(BoothQueue.class))).willReturn(created);

        FestivalQueueListView view = service.getQueues(festivalPublicId, principal);

        assertThat(view.queues()).hasSize(1);
        assertThat(view.queues().getFirst().boothName()).isEqualTo("김밥부스");
        assertThat(view.queues().getFirst().queueId()).isEqualTo(created.getPublicId());
        assertThat(view.queues().getFirst().tailLatitude()).isNull();
    }
}
