package com.example.chookjibupadmin.report.analysis.infrastructure.openai;

import com.example.chookjibupadmin.report.analysis.application.FestivalReportAnalysisException;
import com.example.chookjibupadmin.report.analysis.application.port.FestivalReportAnalysisPort;
import com.example.chookjibupadmin.report.support.dto.FestivalReportAiResult;
import com.example.chookjibupadmin.report.support.dto.FestivalReportEvaluationAi;
import com.example.chookjibupadmin.report.support.dto.FestivalReportMetrics;
import com.example.chookjibupadmin.report.support.dto.FestivalReportTextSummary;
import com.example.chookjibupadmin.report.support.dto.FestivalReviewMetrics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(
        prefix = "app.report.analysis",
        name = "provider",
        havingValue = "openai"
)
public class OpenAiFestivalReportAnalysisAdapter
        implements FestivalReportAnalysisPort {

    private static final String PROMPT = """
            당신은 축제 운영 결과 보고서 작성 도우미입니다.
            제공된 집계 지표와 리뷰 샘플만 근거로 한국어 문장을 작성하세요.
            없는 수치를 만들지 마세요. 리뷰가 없으면 감성은 NONE, 키워드와 평가는 비우세요.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ReportAnalysisProperties properties;

    public OpenAiFestivalReportAnalysisAdapter(
            @Qualifier("openAiReportRestClient") RestClient restClient,
            ObjectMapper objectMapper,
            ReportAnalysisProperties properties
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public FestivalReportAiResult analyze(
            FestivalReportMetrics metrics,
            FestivalReviewMetrics reviews
    ) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "metrics", metrics,
                    "reviews", reviews
            ));
            String body = restClient.post()
                    .uri("/v1/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request(payload))
                    .retrieve()
                    .body(String.class);
            return parse(objectMapper.readTree(body));
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            throw new FestivalReportAnalysisException(
                    "OPENAI_HTTP_" + status,
                    "OpenAI report request failed",
                    status == 429 || status >= 500,
                    exception
            );
        } catch (FestivalReportAnalysisException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new FestivalReportAnalysisException(
                    "OPENAI_RESPONSE_INVALID",
                    "OpenAI report response is invalid",
                    false,
                    exception
            );
        }
    }

    private Map<String, Object> request(String payload) {
        return Map.of(
                "model", properties.modelOrDefault(),
                "store", false,
                "input", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "input_text", "text", PROMPT),
                                Map.of("type", "input_text", "text", payload)
                        )
                )),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "festival_report_ai",
                        "strict", true,
                        "schema", schema()
                ))
        );
    }

    private Map<String, Object> schema() {
        Map<String, Object> stringArray = Map.of(
                "type", "array",
                "items", Map.of("type", "string")
        );
        Map<String, Object> summary = Map.of(
                "type", "object",
                "properties", Map.of(
                        "positives", stringArray,
                        "issues", stringArray,
                        "improvements", stringArray
                ),
                "required", List.of("positives", "issues", "improvements"),
                "additionalProperties", false
        );
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "performanceSummary", summary,
                        "evaluation", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "headlineSentiment", Map.of(
                                                "type", "string",
                                                "enum", List.of(
                                                        "POSITIVE",
                                                        "NEGATIVE",
                                                        "NEUTRAL",
                                                        "NONE"
                                                )
                                        ),
                                        "keywords", Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                        "positive", stringArray,
                                                        "negative", stringArray
                                                ),
                                                "required", List.of(
                                                        "positive",
                                                        "negative"
                                                ),
                                                "additionalProperties", false
                                        ),
                                        "summary", summary
                                ),
                                "required", List.of(
                                        "headlineSentiment",
                                        "keywords",
                                        "summary"
                                ),
                                "additionalProperties", false
                        )
                ),
                "required", List.of("performanceSummary", "evaluation"),
                "additionalProperties", false
        );
    }

    private FestivalReportAiResult parse(JsonNode response) {
        String text = extractOutputText(response);
        try {
            JsonNode root = objectMapper.readTree(text);
            JsonNode performance = root.path("performanceSummary");
            JsonNode evaluation = root.path("evaluation");
            JsonNode keywords = evaluation.path("keywords");
            return new FestivalReportAiResult(
                    readSummary(performance),
                    new FestivalReportEvaluationAi(
                            evaluation.path("headlineSentiment").asText("NONE"),
                            readStringList(keywords.path("positive")),
                            readStringList(keywords.path("negative")),
                            readSummary(evaluation.path("summary"))
                    )
            );
        } catch (Exception exception) {
            throw new FestivalReportAnalysisException(
                    "OPENAI_OUTPUT_PARSE_FAILED",
                    "OpenAI output text missing or invalid",
                    false,
                    exception
            );
        }
    }

    private FestivalReportTextSummary readSummary(JsonNode node) {
        return new FestivalReportTextSummary(
                readStringList(node.path("positives")),
                readStringList(node.path("issues")),
                readStringList(node.path("improvements"))
        );
    }

    private List<String> readStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return values;
        }
        for (JsonNode item : node) {
            if (item != null && item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText().trim());
            }
        }
        return values;
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            throw new FestivalReportAnalysisException(
                    "OPENAI_EMPTY_RESPONSE",
                    "OpenAI returned no response",
                    false
            );
        }
        JsonNode output = response.path("output");
        if (!output.isArray()) {
            throw new FestivalReportAnalysisException(
                    "OPENAI_OUTPUT_MISSING",
                    "OpenAI output text missing",
                    false
            );
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode part : content) {
                if ("output_text".equals(part.path("type").asText())
                        || part.has("text")) {
                    builder.append(part.path("text").asText(""));
                }
            }
        }
        String text = builder.toString().trim();
        if (text.isEmpty()) {
            throw new FestivalReportAnalysisException(
                    "OPENAI_OUTPUT_MISSING",
                    "OpenAI output text missing",
                    false
            );
        }
        return text;
    }
}
