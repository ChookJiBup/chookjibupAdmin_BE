package com.example.demoadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.application.port.MapImageAnalysisRequest;
import com.example.demoadmin.map.command.domain.MapObjectType;
import com.example.demoadmin.map.command.domain.MapStorageType;
import com.example.demoadmin.map.command.infrastructure.openai.OpenAiMapImageAnalyzer;
import com.example.demoadmin.map.command.infrastructure.openai.OpenAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenAiMapImageAnalyzerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("analyze")
    class Analyze {

        @Test
        @DisplayName("OpenAI Responses API 응답을 지도 객체 분석 결과로 변환한다")
        void success_Analyze() throws IOException {
            // given
            HttpServer server = server();
            try {
                OpenAiMapImageAnalyzer analyzer = new OpenAiMapImageAnalyzer(
                        new OpenAiProperties(
                                "test-api-key",
                                "http://localhost:" + server.getAddress().getPort() + "/v1",
                                "gpt-5.6"
                        ),
                        objectMapper
                );

                // when
                var result = analyzer.analyze(request());

                // then
                assertThat(result.objects()).hasSize(1);
                assertThat(result.objects().getFirst().type()).isEqualTo(MapObjectType.FOOD_BOOTH);
                assertThat(result.objects().getFirst().name()).isEqualTo("김밥 부스");
            } finally {
                server.stop(0);
            }
        }

        @Test
        @DisplayName("API Key가 없으면 분석할 수 없다")
        void fail_Analyze_CustomException_ApiKeyBlank() {
            // given
            OpenAiMapImageAnalyzer analyzer = new OpenAiMapImageAnalyzer(
                    new OpenAiProperties(" ", "http://localhost", "gpt-5.6"),
                    objectMapper
            );

            // when & then
            assertThatThrownBy(() -> analyzer.analyze(request()))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        }

        @Test
        @DisplayName("테스트 리소스 외 저장소는 아직 분석할 수 없다")
        void fail_Analyze_CustomException_StorageTypeUnsupported() throws IOException {
            // given
            HttpServer server = server();
            try {
                OpenAiMapImageAnalyzer analyzer = new OpenAiMapImageAnalyzer(
                        new OpenAiProperties(
                                "test-api-key",
                                "http://localhost:" + server.getAddress().getPort() + "/v1",
                                "gpt-5.6"
                        ),
                        objectMapper
                );
                MapImageAnalysisRequest request = new MapImageAnalysisRequest(
                        UUID.randomUUID(),
                        "images/김밥축제_지적편집도.png",
                        MapStorageType.S3,
                        1745,
                        1577
                );

                // when & then
                assertThatThrownBy(() -> analyzer.analyze(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
            } finally {
                server.stop(0);
            }
        }
    }

    private HttpServer server() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/responses", this::handleResponses);
        server.start();
        return server;
    }

    private void handleResponses(HttpExchange exchange) throws IOException {
        String requestBody = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );
        if (!requestBody.contains("data:image/png;base64")) {
            exchange.sendResponseHeaders(400, 0);
            exchange.close();
            return;
        }

        String responseBody = """
                {
                  "output_text": "{\\"objects\\":[{\\"type\\":\\"FOOD_BOOTH\\",\\"name\\":\\"김밥 부스\\",\\"geometry\\":{\\"type\\":\\"RECTANGLE\\",\\"x\\":0.31,\\"y\\":0.22,\\"width\\":0.08,\\"height\\":0.05},\\"confidence\\":0.82}]}"
                }
                """;
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private MapImageAnalysisRequest request() {
        return new MapImageAnalysisRequest(
                UUID.randomUUID(),
                "images/김밥축제_지적편집도.png",
                MapStorageType.TEST_RESOURCE,
                1745,
                1577
        );
    }
}
