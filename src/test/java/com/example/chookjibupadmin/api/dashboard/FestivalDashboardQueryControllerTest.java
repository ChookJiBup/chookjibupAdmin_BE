package com.example.chookjibupadmin.api.dashboard;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.support.AdminHttpIntegrationTestSupport;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FestivalDashboardQueryControllerTest extends AdminHttpIntegrationTestSupport {

    @Nested
    @DisplayName("getDashboard")
    class GetDashboard {

        @Test
        @DisplayName("부스·혼잡이 없으면 대시보드는 200이지만 dataAvailable이 false이다")
        void success_GetDashboard_EmptyMetrics() throws Exception {
            AdminAccount owner = persistOwner();
            CreatedFestival created = createFestival(
                    owner,
                    "대시보드검증축제-" + UUID.randomUUID(),
                    LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(1),
                    null
            );

            mockMvc.perform(get("/api/festivals/{festivalId}/dashboard", created.festivalId())
                            .header("Authorization", bearer(owner)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.festivalName").isNotEmpty())
                    .andExpect(jsonPath("$.data.dataAvailable").value(false))
                    .andExpect(jsonPath("$.data.visitorAvailable").value(false))
                    .andExpect(jsonPath("$.data.currentVisitorCount").value(nullValue()));
        }
    }
}
