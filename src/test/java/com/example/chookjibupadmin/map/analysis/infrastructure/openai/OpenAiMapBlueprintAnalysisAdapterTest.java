package com.example.chookjibupadmin.map.analysis.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.chookjibupadmin.map.analysis.application.MapAnalysisException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiMapBlueprintAnalysisAdapterTest {

    @Test
    @DisplayName("구조화된 OpenAI 응답을 분석 노드로 변환한다")
    void success_Analyze_StructuredOutput() throws Exception {
        // given
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.openai.com");
        MockRestServiceServer server = MockRestServiceServer
                .bindTo(builder)
                .build();
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(
                        Matchers.containsString("\"detail\":\"original\"")
                ))
                .andRespond(withSuccess(
                        structuredResponse(),
                        MediaType.APPLICATION_JSON
                ));

        // when
        var result = adapter(builder.build()).analyze(
                new byte[]{1},
                "image/jpeg",
                100,
                100
        );

        // then
        assertThat(result.nodes()).hasSize(1);
        assertThat(result.nodes().getFirst().name()).isEqualTo("부스 1");
        server.verify();
    }

    @Test
    @DisplayName("OpenAI가 분석을 거부하면 재시도 불가능 오류로 변환한다")
    void fail_Analyze_Refusal_MapAnalysisException() {
        // given
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.openai.com");
        MockRestServiceServer server = MockRestServiceServer
                .bindTo(builder)
                .build();
        server.expect(anything()).andRespond(withSuccess(
                """
                {"status":"completed","output":[{"content":[{"type":"refusal"}]}]}
                """,
                MediaType.APPLICATION_JSON
        ));

        // when & then
        assertThatThrownBy(() -> adapter(builder.build()).analyze(
                new byte[]{1},
                "image/jpeg",
                1,
                1
        )).isInstanceOfSatisfying(
                MapAnalysisException.class,
                exception -> {
                    assertThat(exception.code()).isEqualTo("OPENAI_REFUSAL");
                    assertThat(exception.retryable()).isFalse();
                }
        );
    }

    private OpenAiMapBlueprintAnalysisAdapter adapter(RestClient client) {
        return new OpenAiMapBlueprintAnalysisAdapter(
                client,
                new ObjectMapper(),
                new MapAnalysisProperties(
                        "openai",
                        URI.create("https://api.openai.com"),
                        "test",
                        "gpt-5.6",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        3,
                        3000,
                        1024
                )
        );
    }

    private String structuredResponse() throws Exception {
        String outputText = """
                {
                  "nodes": [
                    {
                      "nodeType": "BOOTH",
                      "name": "부스 1",
                      "geometryType": "RECTANGLE",
                      "geometry": {
                        "x": 0.1,
                        "y": 0.2,
                        "width": 0.3,
                        "height": 0.2,
                        "rotation": 0,
                        "points": []
                      },
                      "confidence": 0.95,
                      "recognizedText": null
                    }
                  ]
                }
                """;

        return new ObjectMapper().writeValueAsString(Map.of(
                "status", "completed",
                "output", List.of(Map.of(
                        "content", List.of(Map.of(
                                "type", "output_text",
                                "text", outputText
                        ))
                ))
        ));
    }
}
