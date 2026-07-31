package com.example.chookjibupadmin.api.address.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chookjibupadmin.address.query.application.dto.RoadAddressSearchResult;
import com.example.chookjibupadmin.address.query.application.dto.RoadAddressView;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoadAddressSearchResponseTest {

    @Test
    @DisplayName("주소 검색 결과와 페이징 정보를 API 응답으로 변환한다")
    void success_From() {
        // given
        RoadAddressView address = new RoadAddressView(
                "광주광역시 북구 비엔날레로 111 (용봉동)",
                "광주광역시 북구 비엔날레로 111",
                " (용봉동)",
                "광주광역시 북구 용봉동 1",
                "61104",
                "광주비엔날레 전시관",
                "2917011200100010000000001"
        );
        RoadAddressSearchResult result =
                new RoadAddressSearchResult(1, 10, 1, List.of(address));

        // when
        RoadAddressSearchResponse response =
                RoadAddressSearchResponse.from(result);

        // then
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.addresses())
                .extracting(RoadAddressResponse::roadAddress)
                .containsExactly(address.roadAddress());
    }
}
