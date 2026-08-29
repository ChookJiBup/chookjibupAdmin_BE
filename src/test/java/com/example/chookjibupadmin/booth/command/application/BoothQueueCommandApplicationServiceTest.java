package com.example.chookjibupadmin.booth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.booth.command.application.dto.BoothQueueResult;
import com.example.chookjibupadmin.booth.command.application.dto.UpdateBoothQueueCommand;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.booth.command.domain.BoothQueue;
import com.example.chookjibupadmin.booth.command.domain.BoothQueueModifierType;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import java.math.BigDecimal;
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
class BoothQueueCommandApplicationServiceTest {

    @InjectMocks
    private BoothQueueCommandApplicationService service;

    @Mock
    private FestivalOperationAccessService festivalOperationAccessService;

    @Mock
    private BoothQueueService boothQueueService;

    @Mock
    private BoothInfoService boothInfoService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Test
    @DisplayName("스태프는 배정 축제 대기열 줄끝을 수정한다")
    void success_UpdateTail_AsStaff() {
        UUID festivalPublicId = UUID.randomUUID();
        BoothQueue queue = BoothQueue.createEmpty(10L, 7L);
        ReflectionTestUtils.setField(queue, "id", 1L);
        BoothInfo booth = BoothInfo.create(10L, 100L, "김밥부스");
        ReflectionTestUtils.setField(booth, "id", 7L);
        FieldStaffPrincipal staff = new FieldStaffPrincipal(3L, 10L, "s1", 0L);
        UpdateBoothQueueCommand command = new UpdateBoothQueueCommand(
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                15,
                List.of()
        );
        given(festivalOperationAccessService.getAuthorizedFestivalId(festivalPublicId, staff))
                .willReturn(10L);
        given(boothQueueService.getByPublicId(queue.getPublicId())).willReturn(queue);
        given(boothInfoService.getById(7L)).willReturn(booth);
        given(boothQueueService.save(any())).willAnswer(inv -> inv.getArgument(0));

        BoothQueueResult result = service.updateTail(
                festivalPublicId,
                queue.getPublicId(),
                command,
                staff
        );

        assertThat(result.queueTailMeters()).isEqualTo(15);
        assertThat(result.lastModifierType()).isEqualTo(BoothQueueModifierType.STAFF);
        assertThat(result.tailLatitude()).isEqualByComparingTo("37.5665");
    }

    @Test
    @DisplayName("한국 인근 범위 밖 줄끝 좌표는 거절한다")
    void fail_UpdateTail_OutOfKorea() {
        UUID festivalPublicId = UUID.randomUUID();
        BoothQueue queue = BoothQueue.createEmpty(10L, 7L);
        BoothInfo booth = BoothInfo.create(10L, 100L, "김밥부스");
        ReflectionTestUtils.setField(booth, "id", 7L);
        FieldStaffPrincipal staff = new FieldStaffPrincipal(3L, 10L, "s1", 0L);
        given(festivalOperationAccessService.getAuthorizedFestivalId(festivalPublicId, staff))
                .willReturn(10L);
        given(boothQueueService.getByPublicId(queue.getPublicId())).willReturn(queue);
        given(boothInfoService.getById(7L)).willReturn(booth);

        assertThatThrownBy(() -> service.updateTail(
                festivalPublicId,
                queue.getPublicId(),
                new UpdateBoothQueueCommand(
                        new BigDecimal("35.0"),
                        new BigDecimal("139.0"),
                        10,
                        null
                ),
                staff
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FESTIVAL_LOCATION_COORDINATES_OUT_OF_KOREA.getMessage());
    }

    @Test
    @DisplayName("다른 축제 대기열은 찾을 수 없다")
    void fail_UpdateTail_WrongFestival() {
        UUID festivalPublicId = UUID.randomUUID();
        BoothQueue queue = BoothQueue.createEmpty(99L, 7L);
        AdminPrincipal admin = new AdminPrincipal(1L, "a@mapo.go.kr");
        given(festivalOperationAccessService.getAuthorizedFestivalId(festivalPublicId, admin))
                .willReturn(10L);
        given(boothQueueService.getByPublicId(queue.getPublicId())).willReturn(queue);

        assertThatThrownBy(() -> service.updateTail(
                festivalPublicId,
                queue.getPublicId(),
                new UpdateBoothQueueCommand(
                        new BigDecimal("37.5665"),
                        new BigDecimal("126.9780"),
                        1,
                        null
                ),
                admin
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.BOOTH_QUEUE_NOT_FOUND.getMessage());
    }
}
