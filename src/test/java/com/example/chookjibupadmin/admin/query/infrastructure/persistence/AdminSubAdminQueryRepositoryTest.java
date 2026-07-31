package com.example.chookjibupadmin.admin.query.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
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
    @DisplayName("searchInvitedSubAdmins")
    class SearchInvitedSubAdmins {

        @Test
        @DisplayName("같은 축제에서 해당 제1 관리자가 초대한 활성 서브관리자만 조회한다")
        void success_SearchInvitedSubAdmins_ActiveSubAdmins() {
            // given
            Long invitedByAdminId = 1L;
            AdminAccount first = subAdmin("sub1@mapo.go.kr", 1L, invitedByAdminId);
            AdminAccount second = subAdmin("sub2@mapo.go.kr", 1L, invitedByAdminId);
            AdminAccount otherFestival = subAdmin("other@mapo.go.kr", 2L, invitedByAdminId);
            AdminAccount otherInviter = subAdmin("other-inviter@mapo.go.kr", 1L, 2L);
            AdminAccount owner = owner("owner@mapo.go.kr", 1L);
            AdminAccount deleted = subAdmin("deleted@mapo.go.kr", 1L, invitedByAdminId);
            deleted.withdraw();
            persist(first);
            persist(second);
            persist(otherFestival);
            persist(otherInviter);
            persist(owner);
            persist(deleted);

            // when
            var result = queryRepository.searchInvitedSubAdmins(
                    1L,
                    invitedByAdminId,
                    null
            );

            // then
            assertThat(result)
                    .extracting(AdminSubAdminView::email)
                    .containsExactly("sub1@mapo.go.kr", "sub2@mapo.go.kr");
        }

        @Test
        @DisplayName("검색어가 있으면 이메일, 이름, 조직으로 필터링한다")
        void success_SearchInvitedSubAdmins_ByKeyword() {
            // given
            Long invitedByAdminId = 1L;
            persist(subAdmin(
                    "sub1@mapo.go.kr",
                    "김검색",
                    "마포구청 소속",
                    1L,
                    invitedByAdminId
            ));
            persist(subAdmin(
                    "sub2@mapo.go.kr",
                    "이관리",
                    "서울시 소속",
                    1L,
                    invitedByAdminId
            ));

            // when
            var result = queryRepository.searchInvitedSubAdmins(
                    1L,
                    invitedByAdminId,
                    "검색"
            );

            // then
            assertThat(result)
                    .extracting(AdminSubAdminView::email)
                    .containsExactly("sub1@mapo.go.kr");
        }

        @Test
        @DisplayName("서브관리자가 없으면 빈 목록을 반환한다")
        void success_SearchInvitedSubAdmins_EmptyBoundary() {
            // given
            Long festivalId = 1L;
            Long invitedByAdminId = 1L;

            // when
            var result = queryRepository.searchInvitedSubAdmins(
                    festivalId,
                    invitedByAdminId,
                    null
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
            AdminAccount saved = persist(subAdmin(
                    "sub@mapo.go.kr",
                    1L,
                    invitedByAdminId
            ));

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
            AdminAccount saved = persist(subAdmin(
                    "sub@mapo.go.kr",
                    2L,
                    invitedByAdminId
            ));

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
            persist(saved);

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

    private AdminAccount owner(String email, Long festivalId) {
        return AdminAccount.createFestivalOwner(
                AdminEmail.of(email),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                festivalId,
                AdminPasswordHash.of("encoded-password")
        );
    }

    private AdminAccount subAdmin(
            String email,
            Long festivalId,
            Long invitedByAdminId
    ) {
        return subAdmin(
                email,
                "김관리",
                "마포구청 소속",
                festivalId,
                invitedByAdminId
        );
    }

    private AdminAccount subAdmin(
            String email,
            String name,
            String organization,
            Long festivalId,
            Long invitedByAdminId
    ) {
        return AdminAccount.createSubAdmin(
                AdminEmail.of(email),
                AdminName.of(name),
                AdminOrganization.of(organization),
                festivalId,
                AdminPasswordHash.of("encoded-password"),
                invitedByAdminId
        );
    }

    private AdminAccount persist(AdminAccount adminAccount) {
        entityManager.persist(adminAccount);
        entityManager.flush();
        if (adminAccount.getFestivalId() != null
                && adminAccount.getRole() == AdminRole.FESTIVAL_OWNER) {
            entityManager.persist(AdminFestivalRole.createFestivalOwner(
                    adminAccount.getId(),
                    adminAccount.getFestivalId()
            ));
        }
        if (adminAccount.getFestivalId() != null
                && adminAccount.getRole() == AdminRole.SUB_ADMIN) {
            entityManager.persist(AdminFestivalRole.createSubAdmin(
                    adminAccount.getId(),
                    adminAccount.getFestivalId(),
                    adminAccount.getInvitedByAdminId()
            ));
        }
        entityManager.flush();
        return adminAccount;
    }
}
