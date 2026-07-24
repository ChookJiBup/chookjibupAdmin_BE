package com.example.demoadmin.admin.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.admin.command.domain.AdminFestivalRole;
import com.example.demoadmin.admin.command.domain.AdminRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class AdminFestivalRoleJpaRepositoryTest {

    @Autowired
    private AdminFestivalRoleJpaRepository adminFestivalRoleJpaRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("관리자 축제 역할을 DB에 저장한다")
        void success_Save() {
            // given
            AdminFestivalRole role = AdminFestivalRole.createFestivalOwner(1L, 1L);

            // when
            AdminFestivalRole saved = adminFestivalRoleJpaRepository.save(role);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getPublicId()).isNotNull();
            assertThat(saved.getRole()).isEqualTo(AdminRole.FESTIVAL_OWNER);
        }
    }

    @Nested
    @DisplayName("findByAdminAccountIdAndFestivalId")
    class FindByAdminAccountIdAndFestivalId {

        @Test
        @DisplayName("관리자 ID와 축제 ID로 역할을 조회한다")
        void success_FindByAdminAccountIdAndFestivalId() {
            // given
            adminFestivalRoleJpaRepository.save(AdminFestivalRole.createSubAdmin(
                    2L,
                    1L,
                    1L
            ));

            // when
            var found = adminFestivalRoleJpaRepository.findByAdminAccountIdAndFestivalId(
                    2L,
                    1L
            );

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(AdminFestivalRole::getRole)
                    .isEqualTo(AdminRole.SUB_ADMIN);
        }
    }

    @Nested
    @DisplayName("existsByFestivalIdAndRole")
    class ExistsByFestivalIdAndRole {

        @Test
        @DisplayName("축제별 1관리자가 있으면 true를 반환한다")
        void success_ExistsByFestivalIdAndRole_ExistingOwner() {
            // given
            adminFestivalRoleJpaRepository.save(AdminFestivalRole.createFestivalOwner(1L, 1L));

            // when
            boolean exists = adminFestivalRoleJpaRepository.existsByFestivalIdAndRole(
                    1L,
                    AdminRole.FESTIVAL_OWNER
            );

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("축제 ID 최소 경계값에서 저장된 역할이 없으면 false를 반환한다")
        void success_ExistsByFestivalIdAndRole_MinFestivalIdBoundary() {
            // given
            Long festivalId = 1L;

            // when
            boolean exists = adminFestivalRoleJpaRepository.existsByFestivalIdAndRole(
                    festivalId,
                    AdminRole.FESTIVAL_OWNER
            );

            // then
            assertThat(exists).isFalse();
        }
    }
}
