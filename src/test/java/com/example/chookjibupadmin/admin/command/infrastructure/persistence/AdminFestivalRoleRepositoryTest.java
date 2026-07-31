package com.example.chookjibupadmin.admin.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRoleRepository;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminFestivalRoleRepositoryTest {

    @Autowired
    private AdminFestivalRoleRepository adminFestivalRoleRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("관리자 축제 역할을 저장한다")
        void success_Save() {
            // given
            AdminFestivalRole role = AdminFestivalRole.createFestivalOwner(1L, 1L);

            // when
            AdminFestivalRole saved = adminFestivalRoleRepository.save(role);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getRole()).isEqualTo(AdminRole.FESTIVAL_OWNER);
        }
    }

    @Nested
    @DisplayName("existsByAdminAccountIdAndFestivalId")
    class ExistsByAdminAccountIdAndFestivalId {

        @Test
        @DisplayName("같은 축제에 이미 배정된 관리자이면 true를 반환한다")
        void success_ExistsByAdminAccountIdAndFestivalId_ExistingRole() {
            // given
            adminFestivalRoleRepository.save(AdminFestivalRole.createSubAdmin(
                    2L,
                    1L,
                    1L
            ));

            // when
            boolean exists = adminFestivalRoleRepository.existsByAdminAccountIdAndFestivalId(
                    2L,
                    1L
            );

            // then
            assertThat(exists).isTrue();
        }
    }
}
