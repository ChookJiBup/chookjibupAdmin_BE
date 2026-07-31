package com.example.chookjibupadmin.admin.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminDepartment;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.admin.query.application.dto.AdminSubAdminCandidateView;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminSubAdminCandidateQueryApplicationServiceIntegrationTest {

    @Autowired
    private AdminSubAdminCandidateQueryApplicationService applicationService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private FestivalService festivalService;

    @Nested
    @DisplayName("searchCandidates")
    class SearchCandidates {

        @Test
        @DisplayName("제1 관리자가 초대 가능한 가입 관리자 후보를 검색한다")
        void success_SearchCandidates_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount owner = adminAccountService.save(owner(festival.getId()));
            adminAccountService.save(admin(
                    "candidate1@mapo.go.kr",
                    "김후보",
                    "마포구청 소속"
            ));
            adminAccountService.save(admin(
                    "candidate2@seoul.go.kr",
                    "이후보",
                    "서울시 소속"
            ));

            // when
            List<AdminSubAdminCandidateView> result =
                    applicationService.searchCandidates(
                            festival.getPublicId(),
                            "김후",
                            principal(owner)
                    );

            // then
            assertThat(result).singleElement().satisfies(candidate -> {
                assertThat(candidate.name()).isEqualTo("김후보");
                assertThat(candidate.email()).isEqualTo("candidate1@mapo.go.kr");
                assertThat(candidate.department()).isEqualTo("관광정책과");
                assertThat(candidate.rank()).isEqualTo("주무관");
            });
        }

        @Test
        @DisplayName("제1 관리자가 이메일 오타로 초대 후보를 검색한다")
        void success_SearchCandidates_EmailTypo() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount owner = adminAccountService.save(owner(festival.getId()));
            adminAccountService.save(admin(
                    "dlgkrwns213@korea.kr",
                    "이학준",
                    "서울시 소속"
            ));
            adminAccountService.save(admin(
                    "other@korea.kr",
                    "김다른",
                    "서울시 소속"
            ));

            // when
            List<AdminSubAdminCandidateView> result =
                    applicationService.searchCandidates(
                            festival.getPublicId(),
                            "dkkkr",
                            principal(owner)
                    );

            // then
            assertThat(result).singleElement().satisfies(candidate -> {
                assertThat(candidate.name()).isEqualTo("이학준");
                assertThat(candidate.email()).isEqualTo("dlgkrwns213@korea.kr");
                assertThat(candidate.department()).isEqualTo("관광정책과");
                assertThat(candidate.rank()).isEqualTo("주무관");
            });
        }
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(
                adminAccount.getId(),
                adminAccount.getFestivalId(),
                adminAccount.getEmailValue(),
                adminAccount.getRole()
        );
    }

    private AdminAccount owner(Long festivalId) {
        return AdminAccount.createFestivalOwner(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                festivalId,
                AdminPasswordHash.of("encoded-password")
        );
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
                AdminDepartment.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
    }

    private Festival festival() {
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("마포구 대표 지역 축제"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 18)
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
    }
}
