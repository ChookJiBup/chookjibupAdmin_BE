package com.example.chookjibupadmin.api.visitor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.support.AdminHttpIntegrationTestSupport;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class FestivalVisitorCountCommandControllerTest extends AdminHttpIntegrationTestSupport {

    @Nested
    @DisplayName("updateDaily")
    class UpdateDaily {

        @Test
        @DisplayName("TOTAL 모드에서 일자별 입력은 40915를 반환한다")
        void fail_UpdateDaily_TotalMode_40915() throws Exception {
            AdminAccount owner = persistOwner();
            CreatedFestival created = createFestival(
                    owner,
                    "모드불일치축제-" + UUID.randomUUID(),
                    LocalDate.of(2026, 8, 20),
                    LocalDate.of(2026, 8, 22),
                    "TOTAL"
            );

            mockMvc.perform(put(
                            "/api/festivals/{festivalId}/operations/visitors/daily/{visitDate}",
                            created.festivalId(),
                            "2026-08-20"
                    )
                            .header("Authorization", bearer(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"visitorCount\":100}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(40915));
        }
    }

    @Nested
    @DisplayName("update festival visitor mode")
    class UpdateVisitorMode {

        @Test
        @DisplayName("방문 인원 데이터가 있으면 모드 변경은 40917을 반환한다")
        void fail_Update_VisitorModeChange_40917() throws Exception {
            AdminAccount owner = persistOwner();
            CreatedFestival created = createFestival(
                    owner,
                    "모드변경불가축제-" + UUID.randomUUID(),
                    LocalDate.of(2026, 8, 20),
                    LocalDate.of(2026, 8, 22),
                    "DAILY"
            );

            mockMvc.perform(put(
                            "/api/festivals/{festivalId}/operations/visitors/daily/{visitDate}",
                            created.festivalId(),
                            "2026-08-20"
                    )
                            .header("Authorization", bearer(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"visitorCount\":100}"))
                    .andExpect(status().isOk());

            mockMvc.perform(patch("/api/festivals/{festivalId}", created.festivalId())
                            .header("Authorization", bearer(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "모드변경불가축제",
                                      "description": "검증용 축제",
                                      "locations": [
                                        {
                                          "locationId": "%s",
                                          "locationType": "MAIN_VENUE",
                                          "locationName": "메인 행사장",
                                          "roadAddress": "서울특별시 마포구 월드컵로 243",
                                          "detailAddress": "월드컵공원",
                                          "latitude": 37.5683,
                                          "longitude": 126.8973,
                                          "primary": true,
                                          "sortOrder": 0
                                        }
                                      ],
                                      "startDate": "2026-08-20",
                                      "endDate": "2026-08-22",
                                      "operationStartTime": "10:00:00",
                                      "operationEndTime": "21:00:00",
                                      "visitorCountInputMode": "TOTAL"
                                    }
                                    """.formatted(created.locationId())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(40917));
        }
    }

    @Nested
    @DisplayName("generate report")
    class GenerateReport {

        @Test
        @DisplayName("일자 합과 총원이 다르면 보고서 생성은 40916을 반환한다")
        void fail_Generate_VisitorConflict_40916() throws Exception {
            AdminAccount owner = persistOwner();
            CreatedFestival created = createFestival(
                    owner,
                    "리포트충돌축제-" + UUID.randomUUID(),
                    LocalDate.of(2026, 8, 20),
                    LocalDate.of(2026, 8, 21),
                    "DAILY"
            );

            mockMvc.perform(put(
                            "/api/festivals/{festivalId}/operations/visitors/daily/{visitDate}",
                            created.festivalId(),
                            "2026-08-20"
                    )
                            .header("Authorization", bearer(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"visitorCount\":100}"))
                    .andExpect(status().isOk());
            mockMvc.perform(put(
                            "/api/festivals/{festivalId}/operations/visitors/daily/{visitDate}",
                            created.festivalId(),
                            "2026-08-21"
                    )
                            .header("Authorization", bearer(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"visitorCount\":100}"))
                    .andExpect(status().isOk());
            mockMvc.perform(put(
                            "/api/festivals/{festivalId}/operations/visitors/total",
                            created.festivalId()
                    )
                            .header("Authorization", bearer(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"visitorCount\":999}"))
                    .andExpect(status().isOk());

            mockMvc.perform(post(
                            "/api/festivals/{festivalId}/reports/generate",
                            created.festivalId()
                    )
                            .header("Authorization", bearer(owner)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(40916));
        }
    }
}
