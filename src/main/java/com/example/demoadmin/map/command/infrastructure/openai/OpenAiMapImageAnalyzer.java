package com.example.demoadmin.map.command.infrastructure.openai;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.application.port.DetectedMapObject;
import com.example.demoadmin.map.command.application.port.MapAnalysisResult;
import com.example.demoadmin.map.command.application.port.MapImageAnalysisRequest;
import com.example.demoadmin.map.command.application.port.MapImageAnalyzer;
import com.example.demoadmin.map.command.domain.GeometryType;
import com.example.demoadmin.map.command.domain.MapObjectType;
import com.example.demoadmin.map.command.domain.MapStorageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * OpenAI Responses API로 축제 배치도 이미지를 분석한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.map.analysis.provider",
        havingValue = "openai"
)
public class OpenAiMapImageAnalyzer implements MapImageAnalyzer {

    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public MapAnalysisResult analyze(MapImageAnalysisRequest request) {
        validateConfigured();

        String responseBody = requestOpenAi(request);
        String outputText = extractOutputText(responseBody);
        if (outputText.isBlank()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return parseAnalysisResult(outputText);
    }

    private void validateConfigured() {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String requestOpenAi(MapImageAnalysisRequest request) {
        try {
            RestClient restClient = RestClient.builder()
                    .baseUrl(properties.resolvedBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            return restClient.post()
                    .uri("/responses")
                    .body(requestBody(request))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> requestBody(MapImageAnalysisRequest request) {
        return Map.of(
                "model", properties.resolvedModel(),
                "input", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of(
                                        "type", "input_text",
                                        "text", prompt()
                                ),
                                Map.of(
                                        "type", "input_image",
                                        "image_url", dataUrl(request)
                                )
                        )
                )),
                "max_output_tokens", 2000
        );
    }

    private String prompt() {
        return """
                축제 배치도 이미지를 분석해 지도 객체를 JSON으로만 반환해라.
                허용 type: FOOD_BOOTH, RESTROOM, ENTRANCE, ROAD, STAGE, BUILDING, PARKING, QUEUE, UNKNOWN.
                허용 geometry.type: RECTANGLE, POLYGON, LINE, POINT.
                모든 좌표는 0부터 1 사이의 정규화 좌표로 반환한다.
                응답 형식:
                {
                  "objects": [
                    {
                      "type": "FOOD_BOOTH",
                      "name": "김밥 부스",
                      "geometry": {"type":"RECTANGLE","x":0.1,"y":0.2,"width":0.1,"height":0.05},
                      "confidence": 0.8
                    }
                  ]
                }
                """;
    }

    private String dataUrl(MapImageAnalysisRequest request) {
        if (request.storageType() != MapStorageType.TEST_RESOURCE) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        ClassPathResource resource = new ClassPathResource(request.storagePath());
        try (InputStream inputStream = resource.getInputStream()) {
            String encoded = Base64.getEncoder()
                    .encodeToString(inputStream.readAllBytes());
            return mimeType(request.storagePath()) + "," + encoded;
        } catch (IOException exception) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String mimeType(String storagePath) {
        String lowerPath = storagePath.toLowerCase();
        if (lowerPath.endsWith(".png")) {
            return "data:image/png;base64";
        }
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "data:image/jpeg;base64";
        }

        throw new CustomException(ErrorCode.INVALID_REQUEST);
    }

    private String extractOutputText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String outputText = root.path("output_text").asText("");
            if (!outputText.isBlank()) {
                return outputText;
            }

            return extractNestedOutputText(root);
        } catch (JsonProcessingException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractNestedOutputText(JsonNode root) {
        StringBuilder builder = new StringBuilder();
        JsonNode output = root.path("output");
        if (!output.isArray()) {
            return "";
        }

        for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                String text = contentItem.path("text").asText("");
                if (!text.isBlank()) {
                    builder.append(text);
                }
            }
        }

        return builder.toString();
    }

    private MapAnalysisResult parseAnalysisResult(String outputText) {
        try {
            JsonNode root = objectMapper.readTree(outputText);
            JsonNode objects = root.path("objects");
            if (!objects.isArray()) {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }

            List<DetectedMapObject> detectedObjects = new ArrayList<>();
            for (JsonNode object : objects) {
                detectedObjects.add(toDetectedMapObject(object));
            }

            return new MapAnalysisResult(detectedObjects);
        } catch (JsonProcessingException exception) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private DetectedMapObject toDetectedMapObject(JsonNode object) {
        JsonNode geometry = object.path("geometry");
        return new DetectedMapObject(
                enumValue(MapObjectType.class, object.path("type").asText("UNKNOWN")),
                object.path("name").asText("미확인 객체"),
                enumValue(GeometryType.class, geometry.path("type").asText("RECTANGLE")),
                geometryData(geometry),
                object.path("confidence").asDouble(0)
        );
    }

    private String geometryData(JsonNode geometry) {
        try {
            return objectMapper.writeValueAsString(geometry);
        } catch (JsonProcessingException exception) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private <T extends Enum<T>> T enumValue(
            Class<T> enumType,
            String value
    ) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
