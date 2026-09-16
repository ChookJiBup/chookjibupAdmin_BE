package com.example.chookjibupadmin.booth.command.infrastructure.openai;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import com.example.chookjibupadmin.booth.command.application.port.QueuePlanRecommendationPort;
import com.example.chookjibupadmin.booth.command.domain.QueueGeometry;
import com.example.chookjibupadmin.map.analysis.infrastructure.openai.MapAnalysisProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiQueuePlanRecommendationAdapterTest {
    private final QueuePlanRecommendationPort.Input input=new QueuePlanRecommendationPort.Input(
            QueueGeometry.point(BigDecimal.valueOf(37),BigDecimal.valueOf(127)),List.of(),List.of(),40,1);
    private final MapAnalysisProperties properties=new MapAnalysisProperties("openai",URI.create("https://api.openai.com"),
            "test-key","configured-model",null,null,1,0,0);
    @Test void success_Recommend_ParsesCompletedStructuredOutput() throws Exception {
        var mapper=new ObjectMapper();
        var builder=RestClient.builder().baseUrl("https://api.openai.com");
        var server=MockRestServiceServer.bindTo(builder).build();
        String text="{\"path\":[{\"lat\":37,\"lng\":127},{\"lat\":37.0004,\"lng\":127}],\"reason\":\"추천\"}";
        var response=java.util.Map.of("status","completed","output",List.of(java.util.Map.of("type","message",
                "content",List.of(java.util.Map.of("type","output_text","text",text)))));
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("configured-model")))
                .andRespond(withSuccess(mapper.writeValueAsString(response),MediaType.APPLICATION_JSON));
        var adapter=new OpenAiQueuePlanRecommendationAdapter(builder.build(),mapper,properties);
        assertThat(adapter.recommend(input).path()).hasSize(2); server.verify();
    }
    @Test void fail_Recommend_HttpFailureAndDisabledProvider() {
        var builder=RestClient.builder().baseUrl("https://api.openai.com");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/responses")).andRespond(withServerError());
        assertThatThrownBy(() -> new OpenAiQueuePlanRecommendationAdapter(builder.build(),new ObjectMapper(),properties)
                .recommend(input)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> new DisabledQueuePlanRecommendationAdapter().recommend(input)).isInstanceOf(RuntimeException.class);
    }
    @Test void fail_Recommend_RefusalAndIncompleteOutput() {
        var builder=RestClient.builder().baseUrl("https://api.openai.com");
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withSuccess("{\"status\":\"incomplete\",\"output\":[]}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> new OpenAiQueuePlanRecommendationAdapter(builder.build(),new ObjectMapper(),properties)
                .recommend(input)).isInstanceOf(RuntimeException.class);
    }
}
