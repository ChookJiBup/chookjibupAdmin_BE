package com.example.demoadmin.address.query.infrastructure.client;

import com.example.demoadmin.address.query.application.dto.RoadAddressSearchResult;
import com.example.demoadmin.address.query.application.dto.RoadAddressView;
import com.example.demoadmin.address.query.application.port.RoadAddressSearchPort;
import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 행정안전부 주소기반산업지원서비스 검색 API를 호출한다.
 *
 * 자동 재시도는 짧은 시간의 반복 호출 차단 가능성 때문에 적용하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class JusoRoadAddressClient implements RoadAddressSearchPort {

    private static final String SUCCESS_CODE = "0";

    @Qualifier("roadAddressRestClient")
    private final RestClient restClient;
    private final RoadAddressProperties properties;

    @Override
    public RoadAddressSearchResult search(
            String keyword,
            int page,
            int size
    ) {
        validateConfiguration();

        try {
            JusoSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/addrlink/addrLinkApi.do")
                            .queryParam("confmKey", properties.confirmationKey())
                            .queryParam("currentPage", page)
                            .queryParam("countPerPage", size)
                            .queryParam("keyword", keyword)
                            .queryParam("resultType", "json")
                            .build())
                    .retrieve()
                    .body(JusoSearchResponse.class);
            return convert(response, page, size);
        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.ROAD_ADDRESS_SEARCH_FAILED);
        }
    }

    private void validateConfiguration() {
        if (!properties.hasConfirmationKey()) {
            throw new CustomException(
                    ErrorCode.ROAD_ADDRESS_API_KEY_NOT_CONFIGURED
            );
        }
    }

    private RoadAddressSearchResult convert(
            JusoSearchResponse response,
            int page,
            int size
    ) {
        if (response == null
                || response.results() == null
                || response.results().common() == null
                || !SUCCESS_CODE.equals(response.results().common().errorCode())) {
            throw new CustomException(ErrorCode.ROAD_ADDRESS_SEARCH_FAILED);
        }

        JusoResults results = response.results();
        List<RoadAddressView> addresses = results.juso() == null
                ? List.of()
                : results.juso().stream()
                        .map(JusoRoadAddressClient::toView)
                        .toList();

        try {
            return new RoadAddressSearchResult(
                    page,
                    size,
                    Integer.parseInt(results.common().totalCount()),
                    addresses
            );
        } catch (NumberFormatException exception) {
            throw new CustomException(ErrorCode.ROAD_ADDRESS_SEARCH_FAILED);
        }
    }

    private static RoadAddressView toView(JusoAddress address) {
        return new RoadAddressView(
                address.roadAddr(),
                address.roadAddrPart1(),
                address.roadAddrPart2(),
                address.jibunAddr(),
                address.zipNo(),
                address.bdNm(),
                address.bdMgtSn()
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JusoSearchResponse(JusoResults results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JusoResults(
            JusoCommon common,
            List<JusoAddress> juso
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JusoCommon(
            String totalCount,
            String errorCode,
            String errorMessage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JusoAddress(
            String roadAddr,
            String roadAddrPart1,
            String roadAddrPart2,
            String jibunAddr,
            String zipNo,
            String bdNm,
            String bdMgtSn
    ) {
    }
}
