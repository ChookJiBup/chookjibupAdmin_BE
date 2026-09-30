package com.example.chookjibupadmin.festival.command.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.api.festival.dto.UpdateFestivalProgressStatusRequest;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.FestivalRepository;
import com.example.chookjibupadmin.festival.command.infrastructure.FestivalProgressScheduler;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import com.example.chookjibupadmin.global.response.CustomException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalProgressStatusTest {
    @Mock FestivalService festivals;
    @Mock AdminAccountService accounts;
    @Mock AdminFestivalRoleService roles;
    @InjectMocks FestivalApplicationService application;

    @Test
    void ownerCanSetAndRestoreStatusWithLock() {
        UUID id = UUID.randomUUID();
        var account = mock(AdminAccount.class);
        var festival = mock(Festival.class);
        var role = mock(AdminFestivalRole.class);
        when(accounts.getById(1L)).thenReturn(account);
        when(account.getId()).thenReturn(1L);
        when(account.isActive()).thenReturn(true);
        when(festivals.getByPublicIdForUpdate(id)).thenReturn(festival);
        when(festival.getId()).thenReturn(2L);
        when(roles.getByAdminAccountIdAndFestivalId(1L, 2L)).thenReturn(role);
        when(role.canModifyFestivalInfo()).thenReturn(true);
        application.changeProgressStatus(id, FestivalProgressStatus.COMPLETED, new AdminPrincipal(1L, "test@mapo.go.kr"));
        application.changeProgressStatus(id, null, new AdminPrincipal(1L, "test@mapo.go.kr"));
        verify(festival).changeProgressStatus(FestivalProgressStatus.COMPLETED);
        verify(festival).changeProgressStatus(null);
    }

    @Test
    void nonOwnerCannotChangeStatus() {
        UUID id = UUID.randomUUID();
        var account = mock(AdminAccount.class);
        var festival = mock(Festival.class);
        when(accounts.getById(1L)).thenReturn(account);
        when(account.getId()).thenReturn(1L);
        when(account.isActive()).thenReturn(true);
        when(festivals.getByPublicIdForUpdate(id)).thenReturn(festival);
        when(festival.getId()).thenReturn(2L);
        when(roles.getByAdminAccountIdAndFestivalId(1L, 2L)).thenReturn(mock(AdminFestivalRole.class));
        assertThatThrownBy(() -> application.changeProgressStatus(id, FestivalProgressStatus.ONGOING,
                new AdminPrincipal(1L, "test@mapo.go.kr"))).isInstanceOf(CustomException.class);
        verify(festival, never()).changeProgressStatus(any());
    }

    @Test
    void inactiveAndAnonymousCannotChangeStatus() {
        when(accounts.getById(1L)).thenReturn(mock(AdminAccount.class));
        assertThatThrownBy(() -> application.changeProgressStatus(UUID.randomUUID(), null,
                new AdminPrincipal(1L, "test@mapo.go.kr"))).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> application.changeProgressStatus(UUID.randomUUID(), null, null))
                .isInstanceOf(CustomException.class);
        verifyNoInteractions(festivals);
    }

    @Test
    void domainAndDateBoundaries() {
        var festival = mock(Festival.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(festival, "storedProgressStatus", "upcoming");
        var today = LocalDate.of(2026, 9, 30);
        festival.changeProgressStatus(FestivalProgressStatus.COMPLETED);
        assertThat(festival.progressStatus(today)).isEqualTo(FestivalProgressStatus.COMPLETED);
        festival.changeProgressStatus(FestivalProgressStatus.ONGOING);
        assertThat(festival.progressStatus(today)).isEqualTo(FestivalProgressStatus.ONGOING);
        festival.changeProgressStatus(null);
        assertThat(festival.getProgressStatusOverride()).isNull();
        assertThat(FestivalProgressStatus.from(today, today, today)).isEqualTo(FestivalProgressStatus.ONGOING);
        assertThat(FestivalProgressStatus.from(today, today.plusDays(1), today.plusDays(2))).isEqualTo(FestivalProgressStatus.UPCOMING);
        assertThat(FestivalProgressStatus.from(today, today.minusDays(2), today.minusDays(1))).isEqualTo(FestivalProgressStatus.COMPLETED);
        assertThat(FestivalProgressStatus.from(today, null, today)).isNull();
        assertThat(FestivalProgressStatus.resolve("completed", today, today, today)).isEqualTo(FestivalProgressStatus.COMPLETED);
    }

    @Test
    void rejectsAmbiguousRequests() {
        assertThat(new UpdateFestivalProgressStatusRequest(true, null).toOverride()).isNull();
        assertThat(new UpdateFestivalProgressStatusRequest(false, FestivalProgressStatus.UPCOMING).toOverride()).isEqualTo(FestivalProgressStatus.UPCOMING);
        assertThatThrownBy(() -> new UpdateFestivalProgressStatusRequest(false, null).toOverride()).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> new UpdateFestivalProgressStatusRequest(null, null).toOverride()).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> new UpdateFestivalProgressStatusRequest(true, FestivalProgressStatus.ONGOING).toOverride()).isInstanceOf(CustomException.class);
    }

    @Test
    void schedulerUsesTransactionalWrapper() {
        var repository = mock(FestivalRepository.class);
        when(repository.synchronizeProgressStatuses()).thenReturn(2);
        var service = new FestivalService(repository);
        assertThat(service.synchronizeProgressStatuses()).isEqualTo(2);
        new FestivalProgressScheduler(service).synchronize();
        verify(repository, times(2)).synchronizeProgressStatuses();
    }
}
