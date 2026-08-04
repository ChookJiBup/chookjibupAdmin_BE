package com.example.chookjibupadmin.map.analysis.infrastructure.openai;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.example.chookjibupadmin.map.analysis.application.MapAnalysisException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiMapBlueprintAnalysisAdapterTest {
    @Test void parsesStructuredOutput(){
        RestClient.Builder builder=RestClient.builder().baseUrl("https://api.openai.com"); MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/responses")).andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"detail\":\"original\"")))
                .andRespond(withSuccess("""
                {"status":"completed","output":[{"content":[{"type":"output_text","text":"{\\\"nodes\\\":[{\\\"nodeType\\\":\\\"BOOTH\\\",\\\"name\\\":\\\"부스 1\\\",\\\"geometryType\\\":\\\"RECTANGLE\\\",\\\"geometry\\\":{\\\"x\\\":0.1,\\\"y\\\":0.2,\\\"width\\\":0.3,\\\"height\\\":0.2,\\\"rotation\\\":0,\\\"points\\\":[]},\\\"confidence\\\":0.95,\\\"recognizedText\\\":null}]}"}]}]}
                """,MediaType.APPLICATION_JSON));
        var result=adapter(builder.build()).analyze(new byte[]{1},"image/jpeg",100,100);
        assertThat(result.nodes()).hasSize(1); assertThat(result.nodes().getFirst().name()).isEqualTo("부스 1"); server.verify();
    }
    @Test void refusalIsPermanentFailure(){
        RestClient.Builder builder=RestClient.builder().baseUrl("https://api.openai.com"); MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
        server.expect(anything()).andRespond(withSuccess("{\"status\":\"completed\",\"output\":[{\"content\":[{\"type\":\"refusal\"}]}]}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->adapter(builder.build()).analyze(new byte[]{1},"image/jpeg",1,1))
                .isInstanceOfSatisfying(MapAnalysisException.class,e->{assertThat(e.code()).isEqualTo("OPENAI_REFUSAL");assertThat(e.retryable()).isFalse();});
    }
    private OpenAiMapBlueprintAnalysisAdapter adapter(RestClient client){return new OpenAiMapBlueprintAnalysisAdapter(client,
            new ObjectMapper(),new MapAnalysisProperties("openai",URI.create("https://api.openai.com"),"test","gpt-5.6",
            Duration.ofSeconds(1),Duration.ofSeconds(1),3,3000,1024));}
}
