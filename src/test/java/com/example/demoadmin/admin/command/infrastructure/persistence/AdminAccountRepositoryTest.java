package com.example.demoadmin.admin.command.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.admin.command.domain.AdminAccount;
import com.example.demoadmin.admin.command.domain.AdminAccountRepository;
import com.example.demoadmin.admin.command.domain.AdminStatus;
import com.example.demoadmin.admin.command.domain.vo.AdminEmail;
import com.example.demoadmin.admin.command.domain.vo.AdminName;
import com.example.demoadmin.admin.command.domain.vo.AdminOrganization;
import com.example.demoadmin.admin.command.domain.vo.AdminPasswordHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminAccountRepositoryTest {

    @Autowired
    private AdminAccountRepository adminAccountRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("관리자 계정을 저장한다")
        void success_Save() {
            // given
            AdminAccount adminAccount = adminAccount("owner@mapo.go.kr");

            // when
            AdminAccount saved = adminAccountRepository.save(adminAccount);

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getPublicId()).isNotNull();
            assertThat(saved.getEmailValue()).isEqualTo("owner@mapo.go.kr");
            assertThat(saved.getStatus()).isEqualTo(AdminStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("findByPublicId")
    class FindByPublicId {

        @Test
        @DisplayName("관리자 UUID로 관리자 계정을 조회한다")
        void success_FindByPublicId() {
            // given
            AdminAccount saved = adminAccountRepository.save(festivalOwner(
                    "owner@mapo.go.kr"
            ));

            // when
            var found = adminAccountRepository.findByPublicId(saved.getPublicId());

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
        @DisplayName("이메일로 관리자 계정을 조회한다")
        void success_FindByEmail() {
            // given
            adminAccountRepository.save(festivalOwner(
                    "owner@mapo.go.kr"
            ));

            // when
            var found = adminAccountRepository.findByEmail(
                    AdminEmail.of("owner@mapo.go.kr")
            );

            // then
            assertThat(found)
                    .isPresent()
                    .get()
                    .extracting(AdminAccount::getEmailValue)
                    .isEqualTo("owner@mapo.go.kr");
        }
    }

    private AdminAccount festivalOwner(String email) {
        return adminAccount(email);
    }

    private AdminAccount adminAccount(String email) {
        return AdminAccount.createAdmin(
                AdminEmail.of(email),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                AdminPasswordHash.of("encoded-password")
        );
    }
}
