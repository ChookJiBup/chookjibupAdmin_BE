package com.example.chookjibupadmin.admin.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminStatus;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class AdminAccountJpaRepositoryTest {

    @Autowired
    private AdminAccountJpaRepository adminAccountJpaRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("관리자 계정 상태를 DB에 저장한다")
        void success_Save_Status() {
            // given
            AdminAccount adminAccount = adminAccount("owner@mapo.go.kr");

            // when
            AdminAccount saved = adminAccountJpaRepository.save(adminAccount);

            // then
            assertThat(saved.getStatus()).isEqualTo(AdminStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("findByPublicId")
    class FindByPublicId {

        @Test
        @DisplayName("관리자 UUID로 DB에서 조회한다")
        void success_FindByPublicId() {
            // given
            AdminAccount saved = adminAccountJpaRepository.save(festivalOwner(
                    "owner@mapo.go.kr"
            ));

            // when
            var found = adminAccountJpaRepository.findByPublicId(saved.getPublicId());

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(AdminAccount::getEmailValue)
                    .isEqualTo("owner@mapo.go.kr");
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("관리자 계정을 저장하고 이메일로 조회한다")
        void success_FindByEmail_IgnoreCase() {
            // given
            AdminAccount adminAccount = adminAccount("owner@mapo.go.kr");
            adminAccountJpaRepository.save(adminAccount);

            // when
            var found = adminAccountJpaRepository.findByEmail(
                    AdminEmail.of("OWNER@MAPO.GO.KR")
            );

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(AdminAccount::getEmailValue)
                    .isEqualTo("owner@mapo.go.kr");
        }

        @Test
        @DisplayName("저장되지 않은 이메일이면 조회 결과가 없다")
        void success_FindByEmail_NotFoundBoundary() {
            // given
            AdminEmail email = AdminEmail.of("missing@mapo.go.kr");

            // when
            var found = adminAccountJpaRepository.findByEmail(email);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("저장된 이메일이면 true를 반환한다")
        void success_ExistsByEmail_ExistingEmail() {
            // given
            adminAccountJpaRepository.save(festivalOwner(
                    "owner@mapo.go.kr"
            ));

            // when
            boolean exists = adminAccountJpaRepository.existsByEmail(
                    AdminEmail.of("owner@mapo.go.kr")
            );

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("저장되지 않은 이메일이면 false를 반환한다")
        void success_ExistsByEmail_NotExistingEmail() {
            // given
            AdminEmail email = AdminEmail.of("other@mapo.go.kr");

            // when
            boolean exists = adminAccountJpaRepository.existsByEmail(email);

            // then
            assertThat(exists).isFalse();
        }
    }

    private AdminAccount festivalOwner(String email) {
        return adminAccount(email);
    }

    private AdminAccount adminAccount(String email) {
        return AdminAccount.createAdmin(
                AdminEmail.of(email),
                AdminName.of("홍길동"),
                AdminOrganization.of("서울시 소속"),
                AdminPasswordHash.of("{noop}password")
        );
    }
}
