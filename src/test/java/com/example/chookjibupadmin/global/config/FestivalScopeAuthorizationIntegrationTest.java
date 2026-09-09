package com.example.chookjibupadmin.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.AdminFestivalRole;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.support.AdminHttpIntegrationTestSupport;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * 축제 범위 API가 총괄관리자 전용인지 운영자(2관리자)도 되는지 고정한다.
 *
 * <p>프런트 가드는 화면 이동만 막을 뿐 API 직접 호출은 통과하므로
 * 서버가 역할별 기대 응답을 지키는지 여기서 검증한다.</p>
 */
class FestivalScopeAuthorizationIntegrationTest extends AdminHttpIntegrationTestSupport {

    @Autowired
    private FestivalService festivalService;

    @Autowired
    private AdminFestivalRoleService adminFestivalRoleService;

    @Test
    @DisplayName("결과 보고서 조회는 총괄관리자만 열 수 있다")
    void fail_SubAdmin_ReadReports_Forbidden() throws Exception {
        Fixture fixture = fixture();

        for (String path : new String[]{"summary", "status", "performance", "evaluation"}) {
            mockMvc.perform(get(
                            "/api/festivals/{festivalId}/reports/{path}",
                            fixture.festivalId(),
                            path
                    ).header("Authorization", bearer(fixture.subAdmin())))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("결과 보고서 생성은 총괄관리자만 요청할 수 있다")
    void fail_SubAdmin_GenerateReport_Forbidden() throws Exception {
        Fixture fixture = fixture();

        mockMvc.perform(post(
                        "/api/festivals/{festivalId}/reports/generate",
                        fixture.festivalId()
                ).header("Authorization", bearer(fixture.subAdmin())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("총괄관리자의 결과 보고서 조회는 권한 오류가 아니다")
    void success_Owner_ReadReportSummary() throws Exception {
        Fixture fixture = fixture();

        mockMvc.perform(get(
                        "/api/festivals/{festivalId}/reports/summary",
                        fixture.festivalId()
                ).header("Authorization", bearer(fixture.owner())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("부스맵 편집기 조회는 총괄관리자만 열 수 있다")
    void fail_SubAdmin_ReadMapEditor_Forbidden() throws Exception {
        Fixture fixture = fixture();

        mockMvc.perform(get(
                        "/api/festivals/{festivalId}/maps/{mapId}/editor",
                        fixture.festivalId(),
                        UUID.randomUUID()
                ).header("Authorization", bearer(fixture.subAdmin())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("총괄관리자는 배치도가 없을 때 권한 오류가 아니라 404를 받는다")
    void success_Owner_ReadMapEditor_NotForbidden() throws Exception {
        Fixture fixture = fixture();

        mockMvc.perform(get(
                        "/api/festivals/{festivalId}/maps/{mapId}/editor",
                        fixture.festivalId(),
                        UUID.randomUUID()
                ).header("Authorization", bearer(fixture.owner())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("운영자도 대시보드와 현장 스태프 관리는 그대로 쓸 수 있다")
    void success_SubAdmin_OperationScope() throws Exception {
        Fixture fixture = fixture();

        mockMvc.perform(get(
                        "/api/festivals/{festivalId}/dashboard",
                        fixture.festivalId()
                ).header("Authorization", bearer(fixture.subAdmin())))
                .andExpect(status().isOk());

        mockMvc.perform(get(
                        "/api/festivals/{festivalId}/field-staff",
                        fixture.festivalId()
                ).header("Authorization", bearer(fixture.subAdmin())))
                .andExpect(status().isOk());

        mockMvc.perform(post(
                        "/api/festivals/{festivalId}/field-staff",
                        fixture.festivalId()
                )
                        .header("Authorization", bearer(fixture.subAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginId": "%s",
                                  "name": "김스태프",
                                  "phoneNumber": "010-1234-5678"
                                }
                                """.formatted(
                                "st" + UUID.randomUUID().toString()
                                        .replace("-", "").substring(0, 8)
                        )))
                .andExpect(status().isCreated());
    }

    private Fixture fixture() throws Exception {
        AdminAccount owner = persistOwner();
        LocalDate today = LocalDate.now();
        CreatedFestival created = createFestival(
                owner,
                "권한검증축제-" + UUID.randomUUID(),
                today.minusDays(2),
                today.minusDays(1),
                null
        );
        AdminAccount subAdmin = persistOwner();
        adminFestivalRoleService.save(AdminFestivalRole.createSubAdmin(
                subAdmin.getId(),
                festivalService.getByPublicId(created.festivalId()).getId(),
                owner.getId()
        ));
        return new Fixture(owner, subAdmin, created.festivalId());
    }

    private record Fixture(
            AdminAccount owner,
            AdminAccount subAdmin,
            UUID festivalId
    ) {
    }
}
