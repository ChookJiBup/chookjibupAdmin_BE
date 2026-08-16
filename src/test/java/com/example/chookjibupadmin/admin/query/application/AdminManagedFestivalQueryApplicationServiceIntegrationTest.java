package com.example.chookjibupadmin.admin.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminDepartment;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalCondition;
import com.example.chookjibupadmin.admin.query.application.dto.AdminManagedFestivalView;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.festival.command.domain.Festival;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalAddress;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalDescription;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalOperationTime;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalPeriod;
import com.example.chookjibupadmin.festival.support.FestivalProgressStatus;
import java.time.Clock;
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
class AdminManagedFestivalQueryApplicationServiceIntegrationTest {

    @Autowired
    private AdminManagedFestivalQueryApplicationService applicationService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Autowired
    private FestivalService festivalService;

    @Autowired
    private Clock clock;

    @Nested
    @DisplayName("searchManagedFestivals")
    class SearchManagedFestivals {

        @Test
        @DisplayName("인증 관리자가 현재 관리 중인 축제 목록을 조회한다")
        void success_SearchManagedFestivals_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount owner = saveOwner(festival);

            // when
            List<AdminManagedFestivalView> result =
                    applicationService.searchManagedFestivals(
                            new AdminManagedFestivalCondition(
                                    AdminRole.FESTIVAL_OWNER,
                                    2026,
                                    "새우젓"
                            ),
                            principal(owner)
                    );

            // then
            assertThat(result)
                    .extracting(AdminManagedFestivalView::festivalId)
                    .containsExactly(festival.getPublicId());
            assertThat(result)
                    .extracting(AdminManagedFestivalView::progressStatus)
                    .containsExactly(FestivalProgressStatus.from(
                            LocalDate.now(clock),
                            festival.getStartDate(),
                            festival.getEndDate()
                    ));
        }

        @Test
        @DisplayName("제2 관리자가 배정된 축제의 진행 상태 목록을 조회한다")
        void success_SearchManagedFestivals_SubAdmin() {
            // given
            LocalDate today = LocalDate.now(clock);
            Festival festival = festivalService.save(festival(
                    today.minusDays(1),
                    today.plusDays(1)
            ));
            AdminAccount owner = saveOwner(festival);
            AdminAccount subAdmin = adminAccountService.save(AdminAccount.createAdmin(
                    AdminEmail.of("sub@mapo.go.kr"),
                    AdminName.of("김서브"),
                    AdminOrganization.of("마포구청 소속"),
                    AdminDepartment.of("관광정책과"),
                    AdminRank.of("주무관"),
                    AdminPasswordHash.of("encoded-password")
            ));
            adminFestivalRoleService.assignSubAdmin(
                    subAdmin.getId(),
                    festival.getId(),
                    owner.getId()
            );

            // when
            List<AdminManagedFestivalView> result =
                    applicationService.searchManagedFestivals(
                            new AdminManagedFestivalCondition(
                                    AdminRole.SUB_ADMIN,
                                    null,
                                    null,
                                    FestivalProgressStatus.ONGOING
                            ),
                            principal(subAdmin)
                    );

            // then
            assertThat(result)
                    .extracting(AdminManagedFestivalView::role)
                    .containsExactly(AdminRole.SUB_ADMIN);
            assertThat(result)
                    .extracting(AdminManagedFestivalView::progressStatus)
                    .containsExactly(FestivalProgressStatus.ONGOING);
        }
    }

    @Nested
    @DisplayName("getManagedFestival")
    class GetManagedFestival {

        @Test
        @DisplayName("인증 관리자가 현재 관리 중인 축제를 UUID로 조회한다")
        void success_GetManagedFestival_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount owner = saveOwner(festival);

            // when
            AdminManagedFestivalView result =
                    applicationService.getManagedFestival(
                            festival.getPublicId(),
                            principal(owner)
                    );

            // then
            assertThat(result.festivalId()).isEqualTo(festival.getPublicId());
            assertThat(result.role()).isEqualTo(AdminRole.FESTIVAL_OWNER);
        }
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(
                adminAccount.getId(),
                adminAccount.getEmailValue()
        );
    }

    private AdminAccount saveOwner(Festival festival) {
        AdminAccount owner = adminAccountService.save(AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("마포구청 소속"),
                AdminDepartment.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        ));
        adminFestivalRoleService.assignFestivalOwner(
                owner.getId(),
                festival.getId()
        );
        return owner;
    }

    private Festival festival() {
        return festival(
                LocalDate.of(2026, 10, 16),
                LocalDate.of(2026, 10, 18)
        );
    }

    private Festival festival(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return Festival.create(
                1L,
                UUID.randomUUID(),
                FestivalName.of("마포나루 새우젓축제"),
                FestivalDescription.of("마포구 대표 지역 축제"),
                FestivalAddress.of("서울특별시 마포구 월드컵로 243"),
                FestivalPeriod.of(
                        startDate,
                        endDate
                ),
                FestivalOperationTime.of(
                        LocalTime.of(10, 0),
                        LocalTime.of(21, 0)
                )
        );
    }
}
