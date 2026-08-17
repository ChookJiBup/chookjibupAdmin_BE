package com.example.chookjibupadmin.admin.query.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminView;
import com.example.chookjibupadmin.admin.query.repository.AdminSubAdminQueryRepository;
import com.example.chookjibupadmin.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({AdminSubAdminQueryRepositoryImpl.class, QuerydslConfig.class})
class AdminSubAdminQueryRepositoryTest {

    @Autowired
    private AdminSubAdminQueryRepository queryRepository;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("findInvitedSubAdmins")
    class FindInvitedSubAdmins {

        @Test
        @DisplayName("같은 축제에서 해당 제1 관리자가 초대한 활성 서브관리자만 조회한다")
        void success_FindInvitedSubAdmins_ActiveSubAdmins() {
            // given
            Long invitedByAdminId = 1L;
            AdminAccount first = subAdmin("sub1@mapo.go.kr", 1L, invitedByAdminId);
            AdminAccount second = subAdmin("sub2@mapo.go.kr", 1L, invitedByAdminId);
            AdminAccount otherFestival = subAdmin("other@mapo.go.kr", 2L, invitedByAdminId);
            AdminAccount otherInviter = subAdmin("other-inviter@mapo.go.kr", 1L, 2L);
            AdminAccount owner = owner("owner@mapo.go.kr", 1L);
            AdminAccount deleted = subAdmin("deleted@mapo.go.kr", 1L, invitedByAdminId);
            deleted.withdraw();
            entityManager.flush();

            // when
            var result = queryRepository.findInvitedSubAdmins(
                    1L,
                    invitedByAdminId
            );

            // then
            assertThat(result)
                    .extracting(AdminSubAdminView::email)
                    .containsExactly("sub1@mapo.go.kr", "sub2@mapo.go.kr");
        }

        @Test
        @DisplayName("서브관리자의 이름, 이메일, 부서, 직급을 반환한다")
        void success_FindInvitedSubAdmins_EmployeeInformation() {
            // given
            Long invitedByAdminId = 1L;
            persistSubAdminWithEmployeeInfo(
                    "sub1@mapo.go.kr",
                    "김검색",
                    "마포구청",
                    1L,
                    invitedByAdminId
            );
            // when
            var result = queryRepository.findInvitedSubAdmins(
                    1L,
                    invitedByAdminId
            );

            // then
            assertThat(result).singleElement().satisfies(subAdmin -> {
                assertThat(subAdmin.name()).isEqualTo("김검색");
                assertThat(subAdmin.email()).isEqualTo("sub1@mapo.go.kr");
                assertThat(subAdmin.rank()).isEqualTo("주무관");
            });
        }

        @Test
        @DisplayName("서브관리자가 없으면 빈 목록을 반환한다")
        void success_FindInvitedSubAdmins_EmptyBoundary() {
            // given
            Long festivalId = 1L;
            Long invitedByAdminId = 1L;

            // when
            var result = queryRepository.findInvitedSubAdmins(
                    festivalId,
                    invitedByAdminId
            );

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findInvitedSubAdmin")
    class FindInvitedSubAdmin {

        @Test
        @DisplayName("같은 축제에서 해당 제1 관리자가 초대한 활성 서브관리자를 UUID로 조회한다")
        void success_FindInvitedSubAdmin() {
            // given
            Long invitedByAdminId = 1L;
            AdminAccount saved = subAdmin(
                    "sub@mapo.go.kr",
                    1L,
                    invitedByAdminId
            );

            // when
            var result = queryRepository.findInvitedSubAdmin(
                    1L,
                    invitedByAdminId,
                    saved.getPublicId()
            );

            // then
            assertThat(result)
                    .isPresent()
                    .get()
                    .extracting(AdminSubAdminView::email)
                    .isEqualTo("sub@mapo.go.kr");
        }

        @Test
        @DisplayName("다른 축제의 서브관리자는 조회 결과가 없다")
        void success_FindInvitedSubAdmin_DifferentFestival() {
            // given
            Long invitedByAdminId = 1L;
            AdminAccount saved = subAdmin(
                    "sub@mapo.go.kr",
                    2L,
                    invitedByAdminId
            );

            // when
            var result = queryRepository.findInvitedSubAdmin(
                    1L,
                    invitedByAdminId,
                    saved.getPublicId()
            );

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("탈퇴한 서브관리자는 조회 결과가 없다")
        void success_FindInvitedSubAdmin_DeletedSubAdmin() {
            // given
            Long invitedByAdminId = 1L;
            AdminAccount saved = subAdmin("sub@mapo.go.kr", 1L, invitedByAdminId);
            saved.withdraw();
            entityManager.flush();

            // when
            var result = queryRepository.findInvitedSubAdmin(
                    1L,
                    invitedByAdminId,
                    saved.getPublicId()
            );

            // then
            assertThat(result).isEmpty();
        }
    }

    private AdminAccount persistAdmin(
            String email,
            String name,
            String department
    ) {
        AdminAccount adminAccount = AdminAccount.createAdmin(
                AdminEmail.of(email),
                AdminName.of(name),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
        entityManager.persist(adminAccount);
        entityManager.flush();
        return adminAccount;
    }

    private AdminAccount owner(String email, Long festivalId) {
        AdminAccount adminAccount = persistAdmin(email, "홍길동", "마포구청");
        entityManager.persist(AdminFestivalRole.createFestivalOwner(
                adminAccount.getId(),
                festivalId
        ));
        entityManager.flush();
        return adminAccount;
    }

    private AdminAccount subAdmin(
            String email,
            Long festivalId,
            Long invitedByAdminId
    ) {
        return subAdmin(
                email,
                "김관리",
                "마포구청",
                festivalId,
                invitedByAdminId
        );
    }

    private AdminAccount subAdmin(
            String email,
            String name,
            String department,
            Long festivalId,
            Long invitedByAdminId
    ) {
        AdminAccount adminAccount = persistAdmin(email, name, department);
        entityManager.persist(AdminFestivalRole.createSubAdmin(
                adminAccount.getId(),
                festivalId,
                invitedByAdminId
        ));
        entityManager.flush();
        return adminAccount;
    }

    private AdminAccount persistSubAdminWithEmployeeInfo(
            String email,
            String name,
            String organization,
            Long festivalId,
            Long invitedByAdminId
    ) {
        AdminAccount adminAccount = AdminAccount.createAdmin(
                AdminEmail.of(email),
                AdminName.of(name),
                AdminOrganization.of(organization),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
        entityManager.persist(adminAccount);
        entityManager.flush();
        entityManager.persist(AdminFestivalRole.createSubAdmin(
                adminAccount.getId(),
                festivalId,
                invitedByAdminId
        ));
        entityManager.flush();
        return adminAccount;
    }
}
