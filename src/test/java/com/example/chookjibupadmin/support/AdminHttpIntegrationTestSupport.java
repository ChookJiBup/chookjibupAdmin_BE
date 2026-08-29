package com.example.chookjibupadmin.support;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.chookjibupadmin.admin.command.application.AdminAccountService;
import com.example.chookjibupadmin.admin.command.domain.AdminAccount;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminName;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminOrganization;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminPasswordHash;
import com.example.chookjibupadmin.admin.command.domain.vo.AdminRank;
import com.example.chookjibupadmin.auth.command.infrastructure.JwtTokenProvider;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
public abstract class AdminHttpIntegrationTestSupport {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private AdminAccountService adminAccountService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    protected AdminAccount persistOwner() {
        return adminAccountService.save(AdminAccount.createAdmin(
                AdminEmail.of("http-" + UUID.randomUUID() + "@mapo.go.kr"),
                AdminName.of("검증관리자"),
                AdminOrganization.of("관광정책과"),
                AdminRank.of("주무관"),
                AdminPasswordHash.of("encoded-password")
        ));
    }

    protected String bearer(AdminAccount adminAccount) {
        return "Bearer " + jwtTokenProvider.createAccessToken(adminAccount);
    }

    protected CreatedFestival createFestival(
            AdminAccount owner,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            String visitorCountInputMode
    ) throws Exception {
        String modeJson = visitorCountInputMode == null
                ? "null"
                : "\"" + visitorCountInputMode + "\"";
        MvcResult result = mockMvc.perform(post("/api/festivals")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "검증용 축제",
                                  "locations": [
                                    {
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
                                  "startDate": "%s",
                                  "endDate": "%s",
                                  "operationStartTime": "10:00:00",
                                  "operationEndTime": "21:00:00",
                                  "visitorCountInputMode": %s
                                }
                                """.formatted(name, startDate, endDate, modeJson)))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return new CreatedFestival(
                UUID.fromString(JsonPath.read(body, "$.data.festivalId")),
                UUID.fromString(JsonPath.read(body, "$.data.locations[0].locationId"))
        );
    }

    protected record CreatedFestival(UUID festivalId, UUID locationId) {
    }
}
