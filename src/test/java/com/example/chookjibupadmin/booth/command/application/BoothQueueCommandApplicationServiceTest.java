package com.example.chookjibupadmin.booth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.booth.command.application.dto.BoothQueueResult;
import com.example.chookjibupadmin.booth.command.application.dto.UpdateBoothQueueCommand;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.booth.command.domain.BoothQueue;
import com.example.chookjibupadmin.booth.command.domain.BoothQueueModifierType;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
import com.example.chookjibupadmin.operator.command.application.FieldStaffAccountService;
import com.example.chookjibupadmin.operator.command.domain.FieldStaffAccount;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffLoginId;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffName;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPasswordHash;
import com.example.chookjibupadmin.operator.command.domain.vo.FieldStaffPhoneNumber;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private BoothCongestionService boothCongestionService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Mock
    private FieldStaffAccountService fieldStaffAccountService;

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
        given(fieldStaffAccountService.getById(3L)).willReturn(staffAccount("김스태프"));
        given(boothQueueService.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(boothCongestionService.findLatestByBoothId(7L))
                .willReturn(Optional.empty());

        BoothQueueResult result = service.updateTail(
                festivalPublicId,
                queue.getPublicId(),
                command,
                staff
        );

        assertThat(result.queueTailMeters()).isEqualTo(15);
        assertThat(result.lastModifierType()).isEqualTo(BoothQueueModifierType.STAFF);
        assertThat(result.lastModifierName()).isEqualTo("김스태프");
        assertThat(result.tailLatitude()).isEqualByComparingTo("37.5665");
        verify(boothCongestionService).save(any(BoothCongestion.class));
    }

    private FieldStaffAccount staffAccount(String name) {
        return FieldStaffAccount.create(
                10L,
                FieldStaffLoginId.of("staff01"),
                FieldStaffName.of(name),
                FieldStaffPhoneNumber.of("010-1234-5678"),
                FieldStaffPasswordHash.of("encoded-password"),
                LocalDateTime.of(2026, 10, 9, 0, 0),
                LocalDateTime.of(2026, 10, 18, 23, 59)
        );
    }

    @Test
    @DisplayName("줄끝 갱신 시 거리 기반 혼잡도 이력을 함께 저장한다")
    void success_UpdateTail_RecordsEstimatedCongestion() {
        UUID festivalPublicId = UUID.randomUUID();
        BoothQueue queue = BoothQueue.createEmpty(10L, 7L);
        ReflectionTestUtils.setField(queue, "id", 1L);
        BoothInfo booth = BoothInfo.create(10L, 100L, "김밥부스");
        ReflectionTestUtils.setField(booth, "id", 7L);
        FieldStaffPrincipal staff = new FieldStaffPrincipal(3L, 10L, "s1", 0L);
        UpdateBoothQueueCommand command = new UpdateBoothQueueCommand(
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                31,
                null
        );
        given(festivalOperationAccessService.getAuthorizedFestivalId(festivalPublicId, staff))
                .willReturn(10L);
        given(boothQueueService.getByPublicId(queue.getPublicId())).willReturn(queue);
        given(boothInfoService.getById(7L)).willReturn(booth);
        given(fieldStaffAccountService.getById(3L)).willReturn(staffAccount("김스태프"));
        given(boothQueueService.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(boothCongestionService.findLatestByBoothId(7L))
                .willReturn(Optional.empty());

        service.updateTail(
                festivalPublicId,
                queue.getPublicId(),
                command,
                staff
        );

        ArgumentCaptor<BoothCongestion> captor = ArgumentCaptor.forClass(
                BoothCongestion.class
        );
        verify(boothCongestionService).save(captor.capture());
        assertThat(captor.getValue().getCongestionLevel())
                .isEqualTo(BoothCongestionLevel.HIGH);
        assertThat(captor.getValue().getWaitMinutes()).isEqualTo(40);
        assertThat(captor.getValue().getModifierStaffId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("관리자 줄끝 갱신 시 관리자 혼잡도 이력을 저장한다")
    void success_UpdateTail_RecordsEstimatedCongestion_AsAdmin() {
        UUID festivalPublicId = UUID.randomUUID();
        BoothQueue queue = BoothQueue.createEmpty(10L, 7L);
        ReflectionTestUtils.setField(queue, "id", 1L);
        BoothInfo booth = BoothInfo.create(10L, 100L, "김밥부스");
        ReflectionTestUtils.setField(booth, "id", 7L);
        AdminPrincipal adminPrincipal = new AdminPrincipal(2L, "admin@example.com");
        AdminAccount admin = AdminAccount.createContractor(
                AdminEmail.of("admin@example.com"),
                AdminName.of("관리자"),
                AdminOrganization.of("운영업체"),
                AdminPasswordHash.of("hash")
        );
        ReflectionTestUtils.setField(admin, "id", 2L);
        UpdateBoothQueueCommand command = new UpdateBoothQueueCommand(
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                31,
                null
        );
        given(festivalOperationAccessService.getAuthorizedFestivalId(
                festivalPublicId,
                adminPrincipal
        )).willReturn(10L);
        given(boothQueueService.getByPublicId(queue.getPublicId())).willReturn(queue);
        given(boothInfoService.getById(7L)).willReturn(booth);
        given(adminAccountService.getById(2L)).willReturn(admin);
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(2L, 10L))
                .willReturn(AdminFestivalRole.createFestivalOwner(2L, 10L));
        given(boothQueueService.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(boothCongestionService.findLatestByBoothId(7L))
                .willReturn(Optional.empty());

        service.updateTail(
                festivalPublicId,
                queue.getPublicId(),
                command,
                adminPrincipal
        );

        ArgumentCaptor<BoothCongestion> captor = ArgumentCaptor.forClass(
                BoothCongestion.class
        );
        verify(boothCongestionService).save(captor.capture());
        assertThat(captor.getValue().getModifierAdminId()).isEqualTo(2L);
        assertThat(captor.getValue().getModifierStaffId()).isNull();
    }

    @Test
    @DisplayName("줄끝 거리가 없으면 혼잡도 이력을 저장하지 않는다")
    void success_UpdateTail_WithoutDistance_SkipsCongestion() {
        UUID festivalPublicId = UUID.randomUUID();
        BoothQueue queue = BoothQueue.createEmpty(10L, 7L);
        ReflectionTestUtils.setField(queue, "id", 1L);
        BoothInfo booth = BoothInfo.create(10L, 100L, "김밥부스");
        ReflectionTestUtils.setField(booth, "id", 7L);
        FieldStaffPrincipal staff = new FieldStaffPrincipal(3L, 10L, "s1", 0L);
        UpdateBoothQueueCommand command = new UpdateBoothQueueCommand(
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                null,
                null
        );
        given(festivalOperationAccessService.getAuthorizedFestivalId(festivalPublicId, staff))
                .willReturn(10L);
        given(boothQueueService.getByPublicId(queue.getPublicId())).willReturn(queue);
        given(boothInfoService.getById(7L)).willReturn(booth);
        given(fieldStaffAccountService.getById(3L)).willReturn(staffAccount("김스태프"));
        given(boothQueueService.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.updateTail(
                festivalPublicId,
                queue.getPublicId(),
                command,
                staff
        );

        verify(boothCongestionService, never()).save(any(BoothCongestion.class));
    }

    @Test
    @DisplayName("직전 혼잡도와 같은 추정값은 중복 저장하지 않는다")
    void success_UpdateTail_SameEstimate_SkipsCongestion() {
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
                null
        );
        BoothCongestion latest = BoothCongestion.recordByStaff(
                7L,
                3L,
                20,
                BoothCongestionLevel.MEDIUM
        );
        given(festivalOperationAccessService.getAuthorizedFestivalId(festivalPublicId, staff))
                .willReturn(10L);
        given(boothQueueService.getByPublicId(queue.getPublicId())).willReturn(queue);
        given(boothInfoService.getById(7L)).willReturn(booth);
        given(fieldStaffAccountService.getById(3L)).willReturn(staffAccount("김스태프"));
        given(boothQueueService.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(boothCongestionService.findLatestByBoothId(7L))
                .willReturn(Optional.of(latest));

        service.updateTail(
                festivalPublicId,
                queue.getPublicId(),
                command,
                staff
        );

        verify(boothCongestionService, never()).save(any(BoothCongestion.class));
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
