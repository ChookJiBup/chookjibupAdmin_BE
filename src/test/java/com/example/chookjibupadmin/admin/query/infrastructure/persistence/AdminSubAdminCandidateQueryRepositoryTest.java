package com.example.chookjibupadmin.admin.query.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
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
    @DisplayName("findCandidates")
    class FindCandidates {

        @Test
        @DisplayName("아직 축제에 배정되지 않은 활성 계약업체만 조회한다")
        void success_FindCandidates_ActiveUnassignedContractors() {
            // given
            Long festivalId = 1L;
            persist(contractor("candidate1@partner.co.kr", "김후보", "가나이벤트"));
            persist(contractor("candidate2@partner.co.kr", "이후보", "다라이벤트"));
            AdminAccount owner = persist(government("owner@mapo.go.kr", "홍길동"));
            entityManager.persist(AdminFestivalRole.createFestivalOwner(
                    owner.getId(),
                    festivalId
            ));
            AdminAccount subAdmin = persist(contractor(
                    "sub@partner.co.kr",
                    "김관리",
                    "마바이벤트"
            ));
            entityManager.persist(AdminFestivalRole.createSubAdmin(
                    subAdmin.getId(),
                    festivalId,
                    owner.getId()
            ));
            AdminAccount deleted = contractor("deleted@partner.co.kr", "박후보", "사아이벤트");
            deleted.withdraw();
            persist(deleted);

            // when
            var result = queryRepository.findCandidates(festivalId);

            // then
            assertThat(result)
                    .extracting(AdminSubAdminCandidateView::email)
                    .containsExactly(
                            "candidate1@partner.co.kr",
                            "candidate2@partner.co.kr"
                    );
        }

        @Test
        @DisplayName("공무원 계정은 배정할 수 없으므로 후보에서 제외한다")
        void success_FindCandidates_ExcludeGovernmentAccounts() {
            // given
            Long festivalId = 1L;
            persist(government("gov1@mapo.go.kr", "김공무"));
            persist(government("gov2@seoul.go.kr", "이공무"));
            persist(contractor("partner@partner.co.kr", "박업체", "가나이벤트"));

            // when
            var result = queryRepository.findCandidates(festivalId);

            // then
            assertThat(result)
                    .extracting(AdminSubAdminCandidateView::email)
                    .containsExactly("partner@partner.co.kr");
        }

        @Test
        @DisplayName("후보자의 이름, 이메일, 업체명을 반환한다")
        void success_FindCandidates_ContractorInformation() {
            // given
            Long festivalId = 1L;
            persist(contractor("candidate2@partner.co.kr", "이검색", "다라이벤트"));

            // when
            var result = queryRepository.findCandidates(festivalId);

            // then
            assertThat(result).singleElement().satisfies(candidate -> {
                assertThat(candidate.name()).isEqualTo("이검색");
                assertThat(candidate.email()).isEqualTo("candidate2@partner.co.kr");
                assertThat(candidate.organization()).isEqualTo("다라이벤트");
                assertThat(candidate.rank()).isNull();
            });
        }

        @Test
        @DisplayName("후보자가 없으면 빈 목록을 반환한다")
        void success_FindCandidates_EmptyBoundary() {
            // given
            Long festivalId = 1L;

            // when
            var result = queryRepository.findCandidates(festivalId);

            // then
            assertThat(result).isEmpty();
        }
    }

    private AdminAccount contractor(
            String email,
            String name,
            String companyName
    ) {
        return AdminAccount.createContractor(
                AdminEmail.of(email),
                AdminName.of(name),
                AdminOrganization.of(companyName),
                AdminPasswordHash.of("encoded-password")
        );
    }

    private AdminAccount government(
            String email,
            String name
    ) {
        return AdminAccount.createGovernment(
                AdminEmail.of(email),
                AdminName.of(name),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
    }

    private AdminAccount persist(AdminAccount adminAccount) {
        entityManager.persist(adminAccount);
        entityManager.flush();
        return adminAccount;
    }
}
