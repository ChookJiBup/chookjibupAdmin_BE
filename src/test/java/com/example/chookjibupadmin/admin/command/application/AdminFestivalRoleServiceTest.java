package com.example.chookjibupadmin.admin.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRoleRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminFestivalRoleServiceTest {

    @InjectMocks
    private AdminFestivalRoleService service;

    @Mock
    private AdminFestivalRoleRepository repository;

    @Nested
    @DisplayName("getAllByAdminAccountIdsAndFestivalId")
    class GetAllByAdminAccountIdsAndFestivalId {

        @Test
        @DisplayName("관리자 ID 목록과 축제 ID에 해당하는 역할을 조회한다")
        void success_GetAllByAdminAccountIdsAndFestivalId() {
            // given
            List<Long> accountIds = List.of(2L, 3L);
            List<AdminFestivalRole> roles = List.of(
                    AdminFestivalRole.createSubAdmin(2L, 1L, 1L),
                    AdminFestivalRole.createSubAdmin(3L, 1L, 1L)
            );
            given(repository.findAllByAdminAccountIdInAndFestivalId(
                    accountIds,
                    1L
            )).willReturn(roles);

            // when
            List<AdminFestivalRole> found =
                    service.getAllByAdminAccountIdsAndFestivalId(accountIds, 1L);

            // then
            assertThat(found).containsExactlyElementsOf(roles);
        }
    }

    @Nested
    @DisplayName("deleteAll")
    class DeleteAll {

        @Test
        @DisplayName("관리자 축제 역할 삭제를 저장소에 위임한다")
        void success_DeleteAll() {
            // given
            List<AdminFestivalRole> roles = List.of(
                    AdminFestivalRole.createSubAdmin(2L, 1L, 1L)
            );

            // when
            service.deleteAll(roles);

            // then
            then(repository).should().deleteAll(roles);
        }
    }
}
