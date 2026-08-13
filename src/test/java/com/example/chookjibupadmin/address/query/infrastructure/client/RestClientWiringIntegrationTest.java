package com.example.chookjibupadmin.address.query.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.map.analysis.infrastructure.openai.OpenAiMapBlueprintAnalysisAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.map.analysis.provider=openai",
        "app.map.analysis.api-key=test-openai-api-key"
})
class RestClientWiringIntegrationTest {

    @Autowired
    private JusoRoadAddressClient jusoRoadAddressClient;

    @Autowired
    private OpenAiMapBlueprintAnalysisAdapter openAiMapBlueprintAnalysisAdapter;

    @Test
    @DisplayName("주소와 OpenAI RestClient가 동시에 존재해도 각각 주입된다")
    void success_ContextLoads_WithMultipleRestClients() {
        assertThat(jusoRoadAddressClient).isNotNull();
        assertThat(openAiMapBlueprintAnalysisAdapter).isNotNull();
    }
}
