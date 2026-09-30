package com.example.chookjibupadmin.api.festival;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.chookjibupadmin.support.AdminHttpIntegrationTestSupport;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class FestivalProgressStatusControllerIntegrationTest extends AdminHttpIntegrationTestSupport {
    @Test
    void ownerChangesStatusAndBothListsUseOverride() throws Exception {
        var owner = persistOwner();
        var festival = createFestival(owner, "상태 검증 축제", LocalDate.now().plusDays(10), LocalDate.now().plusDays(11), null);
        String path = "/api/festivals/" + festival.festivalId() + "/progress-status";
        mockMvc.perform(patch(path).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"automatic":false,"progressStatus":"COMPLETED"}
                """)).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/me/managed-festivals/" + festival.festivalId())
                .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.progressStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.progressStatusOverride").value("COMPLETED"));
        mockMvc.perform(get("/api/admin/me/managed-festivals?progressStatus=COMPLETED")
                .header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].progressStatus").value("COMPLETED"));
        mockMvc.perform(patch(path).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content("{\"automatic\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/me/managed-festivals/" + festival.festivalId())
                .header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.data.progressStatus").value("UPCOMING"));
    }

    @Test
    void invalidAndAnonymousRequestsAreRejected() throws Exception {
        var owner = persistOwner();
        var festival = createFestival(owner, "잘못된 상태 요청 축제", LocalDate.now(), LocalDate.now(), null);
        String path = "/api/festivals/" + festival.festivalId() + "/progress-status";
        for (String body : new String[]{"{}", "{\"automatic\":false}", "{\"automatic\":true,\"progressStatus\":\"ONGOING\"}", "{\"automatic\":false,\"progressStatus\":\"INVALID\"}"}) {
            mockMvc.perform(patch(path).header("Authorization", bearer(owner))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mockMvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content("{\"automatic\":true}"))
                .andExpect(status().isUnauthorized());
    }
}
