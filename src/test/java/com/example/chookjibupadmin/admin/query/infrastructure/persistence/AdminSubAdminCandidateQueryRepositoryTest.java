package com.example.chookjibupadmin.admin.query.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminCandidateView;
import com.example.chookjibupadmin.admin.query.repository.AdminSubAdminCandidateQueryRepository;
import com.example.chookjibupadmin.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({AdminSubAdminCandidateQueryRepositoryImpl.class, QuerydslConfig.class})
class AdminSubAdminCandidateQueryRepositoryTest {

    @Autowired
    private AdminSubAdminCandidateQueryRepository queryRepository;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("searchCandidates")
    class SearchCandidates {

        @Test
        @DisplayName("아직 축제에 배정되지 않은 활성 관리자만 조회한다")
        void success_SearchCandidates_ActiveUnassignedAdmins() {
            // given
            Long festivalId = 1L;
            persist(admin("candidate1@mapo.go.kr", "김후보", "마포구청 소속"));
            persist(admin("candidate2@mapo.go.kr", "이후보", "서울시 소속"));
            AdminAccount owner = persist(admin("owner@mapo.go.kr", "홍길동", "마포구청 소속"));
            entityManager.persist(AdminFestivalRole.createFestivalOwner(
                    owner.getId(),
                    festivalId
            ));
            AdminAccount subAdmin = persist(admin("sub@mapo.go.kr", "김관리", "마포구청 소속"));
            entityManager.persist(AdminFestivalRole.createSubAdmin(
                    subAdmin.getId(),
                    festivalId,
                    owner.getId()
            ));
            AdminAccount deleted = admin("deleted@mapo.go.kr", "박후보", "마포구청 소속");
            deleted.withdraw();
            persist(deleted);

            // when
            var result = queryRepository.searchCandidates(festivalId, null);

            // then
            assertThat(result)
                    .extracting(AdminSubAdminCandidateView::email)
                    .containsExactly(
                            "candidate1@mapo.go.kr",
                            "candidate2@mapo.go.kr"
                    );
        }

        @Test
        @DisplayName("검색어가 있으면 이메일, 이름, 조직으로 필터링한다")
        void success_SearchCandidates_ByKeyword() {
            // given
            Long festivalId = 1L;
            persist(admin("candidate1@mapo.go.kr", "김후보", "마포구청 소속"));
            persist(admin("candidate2@seoul.go.kr", "이검색", "서울시 소속"));

            // when
            var result = queryRepository.searchCandidates(festivalId, "검색");

            // then
            assertThat(result)
                    .extracting(AdminSubAdminCandidateView::email)
                    .containsExactly("candidate2@seoul.go.kr");
        }

        @Test
        @DisplayName("후보자가 없으면 빈 목록을 반환한다")
        void success_SearchCandidates_EmptyBoundary() {
            // given
            Long festivalId = 1L;
            String keyword = "없음";

            // when
            var result = queryRepository.searchCandidates(festivalId, keyword);

            // then
            assertThat(result).isEmpty();
        }
    }

    private AdminAccount admin(
            String email,
            String name,
            String organization
    ) {
        return AdminAccount.createAdmin(
                AdminEmail.of(email),
                AdminName.of(name),
                AdminOrganization.of(organization),
                AdminPasswordHash.of("encoded-password")
        );
    }

    private AdminAccount persist(AdminAccount adminAccount) {
        entityManager.persist(adminAccount);
        entityManager.flush();
        return adminAccount;
    }
}
