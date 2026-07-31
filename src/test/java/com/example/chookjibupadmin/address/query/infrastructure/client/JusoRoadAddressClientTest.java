package com.example.chookjibupadmin.address.query.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.chookjibupadmin.address.query.application.dto.RoadAddressSearchResult;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.util.UriUtils;
import org.springframework.web.client.RestClient;

class JusoRoadAddressClientTest {

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("공식 주소 API 요청을 내부 검색 결과로 변환한다")
        void success_Search() {
            // given
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl("https://business.juso.go.kr");
            MockRestServiceServer server =
                    MockRestServiceServer.bindTo(builder).build();
            JusoRoadAddressClient client =
                    new JusoRoadAddressClient(builder.build(), properties("test-key"));
            server.expect(method(HttpMethod.GET))
                    .andExpect(queryParam("confmKey", "test-key"))
                    .andExpect(queryParam("currentPage", "1"))
                    .andExpect(queryParam("countPerPage", "10"))
                    .andExpect(queryParam(
                            "keyword",
                            UriUtils.encodeQueryParam(
                                    "광주비엔날레",
                                    StandardCharsets.UTF_8
                            )
                    ))
                    .andExpect(queryParam("resultType", "json"))
                    .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

            // when
            RoadAddressSearchResult result =
                    client.search("광주비엔날레", 1, 10);

            // then
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.addresses().getFirst().roadAddressPart1())
                    .isEqualTo("광주광역시 북구 비엔날레로 111");
            assertThat(result.addresses().getFirst().buildingManagementNumber())
                    .isEqualTo("2917011200100010000000001");
            server.verify();
        }

        @Test
        @DisplayName("승인키가 없으면 설정 예외를 던진다")
        void fail_Search_ApiKeyNotConfigured_CustomException() {
            // given
            JusoRoadAddressClient client = new JusoRoadAddressClient(
                    RestClient.create(),
                    properties("")
            );

            // when & then
            assertThatThrownBy(() -> client.search("광주비엔날레", 1, 10))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(
                            ErrorCode.ROAD_ADDRESS_API_KEY_NOT_CONFIGURED
                                    .getMessage()
                    );
        }

        @Test
        @DisplayName("외부 API 오류 응답이면 주소 검색 실패 예외를 던진다")
        void fail_Search_ExternalError_CustomException() {
            // given
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl("https://business.juso.go.kr");
            MockRestServiceServer server =
                    MockRestServiceServer.bindTo(builder).build();
            JusoRoadAddressClient client =
                    new JusoRoadAddressClient(builder.build(), properties("test-key"));
            server.expect(method(HttpMethod.GET))
                    .andRespond(withSuccess(errorBody(), MediaType.APPLICATION_JSON));

            // when & then
            assertThatThrownBy(() -> client.search("광주비엔날레", 1, 10))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.ROAD_ADDRESS_SEARCH_FAILED.getMessage());
            server.verify();
        }

        @Test
        @DisplayName("외부 서버가 실패하면 주소 검색 실패 예외를 던진다")
        void fail_Search_ServerError_CustomException() {
            // given
            RestClient.Builder builder = RestClient.builder()
                    .baseUrl("https://business.juso.go.kr");
            MockRestServiceServer server =
                    MockRestServiceServer.bindTo(builder).build();
            JusoRoadAddressClient client =
                    new JusoRoadAddressClient(builder.build(), properties("test-key"));
            server.expect(method(HttpMethod.GET))
                    .andRespond(withServerError());

            // when & then
            assertThatThrownBy(() -> client.search("광주비엔날레", 1, 10))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.ROAD_ADDRESS_SEARCH_FAILED.getMessage());
            server.verify();
        }
    }

    private RoadAddressProperties properties(String confirmationKey) {
        return new RoadAddressProperties(
                "https://business.juso.go.kr",
                confirmationKey,
                Duration.ofSeconds(3),
                Duration.ofSeconds(5)
        );
    }

    private String successBody() {
        return """
                {
                  "results": {
                    "common": {
                      "totalCount": "1",
                      "errorCode": "0",
                      "errorMessage": "정상"
                    },
                    "juso": [{
                      "roadAddr": "광주광역시 북구 비엔날레로 111 (용봉동)",
                      "roadAddrPart1": "광주광역시 북구 비엔날레로 111",
                      "roadAddrPart2": " (용봉동)",
                      "jibunAddr": "광주광역시 북구 용봉동 1",
                      "zipNo": "61104",
                      "bdNm": "광주비엔날레 전시관",
                      "bdMgtSn": "2917011200100010000000001"
                    }]
                  }
                }
                """;
    }

    private String errorBody() {
        return """
                {
                  "results": {
                    "common": {
                      "totalCount": "0",
                      "errorCode": "E0001",
                      "errorMessage": "승인되지 않은 KEY 입니다."
                    },
                    "juso": []
                  }
                }
                """;
    }
}
