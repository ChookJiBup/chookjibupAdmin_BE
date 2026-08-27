package com.example.chookjibupadmin.booth.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.booth.command.application.dto.BoothCongestionResult;
import com.example.chookjibupadmin.booth.command.application.dto.UpdateBoothCongestionCommand;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestion;
import com.example.chookjibupadmin.booth.command.domain.BoothCongestionLevel;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import java.time.LocalDate;
import java.time.LocalTime;
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
class BoothCongestionCommandApplicationServiceTest {

    @InjectMocks
    private BoothCongestionCommandApplicationService service;

    @Mock
    private FestivalService festivalService;

    @Mock
    private BoothInfoService boothInfoService;

    @Mock
    private BoothCongestionService boothCongestionService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private AdminFestivalRoleService adminFestivalRoleService;

    @Test
    @DisplayName("스태프는 배정 축제 부스에 혼잡을 기록한다")
    void success_Record_AsStaff() {
        Festival festival = festival(10L);
        BoothInfo booth = BoothInfo.create(10L, 100L, "김밥부스");
        ReflectionTestUtils.setField(booth, "id", 7L);
        FieldStaffPrincipal staff = new FieldStaffPrincipal(3L, 10L, "s1", 0L);
        given(festivalService.getByPublicId(festival.getPublicId())).willReturn(festival);
        given(boothInfoService.getById(7L)).willReturn(booth);
        given(boothCongestionService.save(any())).willAnswer(inv -> inv.getArgument(0));

        BoothCongestionResult result = service.record(
                festival.getPublicId(),
                7L,
                new UpdateBoothCongestionCommand(15, BoothCongestionLevel.HIGH),
                staff
        );

        ArgumentCaptor<BoothCongestion> captor = ArgumentCaptor.forClass(BoothCongestion.class);
        then(boothCongestionService).should().save(captor.capture());
        assertThat(captor.getValue().getModifierStaffId()).isEqualTo(3L);
        assertThat(result.waitMinutes()).isEqualTo(15);
    }

    @Test
    @DisplayName("타 축제 부스로 혼잡 입력을 거부한다")
    void fail_Record_WrongFestivalBooth() {
        Festival festival = festival(10L);
        BoothInfo booth = BoothInfo.create(99L, 100L, "다른축제부스");
        ReflectionTestUtils.setField(booth, "id", 7L);
        given(festivalService.getByPublicId(festival.getPublicId())).willReturn(festival);
        given(boothInfoService.getById(7L)).willReturn(booth);

        assertThatThrownBy(() -> service.record(
                festival.getPublicId(),
                7L,
                new UpdateBoothCongestionCommand(5, BoothCongestionLevel.LOW),
                new FieldStaffPrincipal(3L, 10L, "s1", 0L)
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.BOOTH_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("관리자는 줄끝 권한이 있을 때 혼잡을 기록한다")
    void success_Record_AsAdmin() {
        Festival festival = festival(10L);
        BoothInfo booth = BoothInfo.create(10L, 100L, "김밥부스");
        ReflectionTestUtils.setField(booth, "id", 7L);
        AdminAccount admin = admin();
        AdminPrincipal principal = new AdminPrincipal(admin.getId(), "hong@korea.kr");
        given(festivalService.getByPublicId(festival.getPublicId())).willReturn(festival);
        given(boothInfoService.getById(7L)).willReturn(booth);
        given(adminAccountService.getById(admin.getId())).willReturn(admin);
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(admin.getId(), 10L))
                .willReturn(AdminFestivalRole.createFestivalOwner(admin.getId(), 10L));
        given(boothCongestionService.save(any())).willAnswer(inv -> inv.getArgument(0));

        BoothCongestionResult result = service.record(
                festival.getPublicId(),
                7L,
                new UpdateBoothCongestionCommand(8, BoothCongestionLevel.MEDIUM),
                principal
        );

        assertThat(result.congestionLevel()).isEqualTo(BoothCongestionLevel.MEDIUM);
    }

    private AdminAccount admin() {
        AdminAccount account = AdminAccount.createAdmin(
                AdminEmail.of("hong@korea.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("hash")
        );
        ReflectionTestUtils.setField(account, "id", 1L);
        return account;
    }

    private Festival festival(Long id) {
        Festival festival = Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("축제"),
                FestivalDescription.of("설명"),
                FestivalAddress.of("서울"),
                FestivalPeriod.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3)),
                FestivalOperationTime.of(LocalTime.of(10, 0), LocalTime.of(20, 0))
        );
        ReflectionTestUtils.setField(festival, "id", id);
        return festival;
    }
}
