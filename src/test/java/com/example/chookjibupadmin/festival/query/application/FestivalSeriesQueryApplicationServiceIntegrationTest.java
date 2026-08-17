package com.example.chookjibupadmin.festival.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.support.AdminPrincipal;
import com.example.chookjibupadmin.festival.command.application.FestivalSeriesService;
import com.example.chookjibupadmin.festival.command.domain.FestivalSeries;
import com.example.chookjibupadmin.festival.command.domain.vo.FestivalName;
import com.example.chookjibupadmin.festival.query.application.dto.FestivalSeriesSearchView;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FestivalSeriesQueryApplicationServiceIntegrationTest {

    @Autowired
    private FestivalSeriesQueryApplicationService applicationService;

    @Autowired
    private FestivalSeriesService festivalSeriesService;

    @Autowired
    private AdminAccountService adminAccountService;

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("저장된 축제 시리즈를 인증 관리자 검색 흐름으로 조회한다")
        void success_Search_Persisted() {
            // given
            FestivalSeries series = festivalSeriesService.save(
                    FestivalSeries.create(FestivalName.of("김밥축제"))
            );
            AdminAccount adminAccount = adminAccountService.save(adminAccount());

            // when
            List<FestivalSeriesSearchView> result =
                    applicationService.search(
                            "김밥",
                            10,
                            principal(adminAccount)
                    );

            // then
            assertThat(result)
                    .extracting(FestivalSeriesSearchView::seriesId)
                    .containsExactly(series.getPublicId());
        }
    }

    private AdminAccount adminAccount() {
        return AdminAccount.createAdmin(
                AdminEmail.of("owner@mapo.go.kr"),
                AdminName.of("홍길동"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        );
    }

    private AdminPrincipal principal(AdminAccount adminAccount) {
        return new AdminPrincipal(
                adminAccount.getId(),
                adminAccount.getEmailValue()
        );
    }
}
