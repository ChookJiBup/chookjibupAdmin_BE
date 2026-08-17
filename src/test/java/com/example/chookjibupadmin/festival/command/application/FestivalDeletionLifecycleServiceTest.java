package com.example.chookjibupadmin.festival.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
import com.example.chookjibupadmin.festival.command.application.dto.FestivalDeletionTarget;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDetailAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.festival.location.application.FestivalLocationService;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.FestivalMapPurgeService;
import com.example.chookjibupadmin.operator.command.application.FieldStaffAccountService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalDeletionLifecycleServiceTest {

    @InjectMocks
    private FestivalDeletionLifecycleService service;

    @Mock private FestivalService festivalService;
    @Mock private AdminAccountService adminAccountService;
    @Mock private AdminFestivalRoleService adminFestivalRoleService;
    @Mock private FestivalLocationService festivalLocationService;
    @Mock private FieldStaffAccountService fieldStaffAccountService;
    @Mock private FestivalMapPurgeService festivalMapPurgeService;

    private final UUID festivalPublicId = UUID.randomUUID();
    private final AdminPrincipal principal = new AdminPrincipal(
            1L,
            "owner@mapo.go.kr"
    );
    private AdminAccount adminAccount;
    private Festival festival;

    @BeforeEach
    void setUp() {
        adminAccount = AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
        ReflectionTestUtils.setField(adminAccount, "id", 1L);
        festival = Festival.create(
                festivalPublicId,
                10L,
                UUID.randomUUID(),
                FestivalName.of("테스트 축제"),
                FestivalDescription.of("설명"),
                FestivalAddress.of("서울특별시 마포구"),
                FestivalDetailAddress.of("월드컵공원"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 3)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(20, 0)
                )
        );
        ReflectionTestUtils.setField(festival, "id", 20L);

        given(adminAccountService.getById(1L)).willReturn(adminAccount);
        given(festivalService.getByPublicIdForUpdate(festivalPublicId))
                .willReturn(festival);
    }

    @Test
    @DisplayName("1관리자는 축제 삭제를 준비할 수 있다")
    void success_BeginDeletion() {
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(1L, 20L))
                .willReturn(AdminFestivalRole.createFestivalOwner(1L, 20L));
        given(festivalMapPurgeService.beginDeletion(20L))
                .willReturn(List.of("original-key"));

        FestivalDeletionTarget target = service.beginDeletion(
                festivalPublicId,
                principal
        );

        assertThat(target.objectKeys()).containsExactly("original-key");
    }

    @Test
    @DisplayName("제2관리자는 축제를 삭제할 수 없다")
    void fail_BeginDeletion_SubAdmin() {
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(1L, 20L))
                .willReturn(AdminFestivalRole.createSubAdmin(1L, 20L, 2L));

        assertThatThrownBy(() -> service.beginDeletion(
                festivalPublicId,
                principal
        )).isInstanceOfSatisfying(
                CustomException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN)
        );

        then(festivalMapPurgeService).should(never()).beginDeletion(20L);
    }

    @Test
    @DisplayName("연관 관리자 데이터를 지운 뒤 축제를 영구 삭제한다")
    void success_CompleteDeletion() {
        AdminFestivalRole owner = AdminFestivalRole.createFestivalOwner(1L, 20L);
        given(adminFestivalRoleService.getByAdminAccountIdAndFestivalId(1L, 20L))
                .willReturn(owner);
        given(festivalLocationService.findAllByFestivalId(20L))
                .willReturn(List.of());
        given(adminFestivalRoleService.getAllByFestivalId(20L))
                .willReturn(List.of(owner));

        service.completeDeletion(festivalPublicId, principal);

        then(festivalMapPurgeService).should().purgeDatabase(20L);
        then(fieldStaffAccountService).should().deleteAllByFestivalId(20L);
        then(adminFestivalRoleService).should().deleteAll(List.of(owner));
        then(festivalService).should().delete(festival);
    }
}
