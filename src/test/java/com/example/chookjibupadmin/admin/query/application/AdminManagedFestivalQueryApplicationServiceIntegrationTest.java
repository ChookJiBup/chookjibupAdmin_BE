package com.example.chookjibupadmin.admin.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminRole;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
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
    private FestivalService festivalService;

    @Nested
    @DisplayName("searchManagedFestivals")
    class SearchManagedFestivals {

        @Test
        @DisplayName("인증 관리자가 현재 관리 중인 축제 목록을 조회한다")
        void success_SearchManagedFestivals_Persisted() {
            // given
            Festival festival = festivalService.save(festival());
            AdminAccount owner = adminAccountService.save(owner(festival.getId()));

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
            AdminAccount owner = adminAccountService.save(owner(festival.getId()));

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
