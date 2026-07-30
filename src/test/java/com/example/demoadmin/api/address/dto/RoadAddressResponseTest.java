package com.example.demoadmin.api.address.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.address.query.application.dto.RoadAddressView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoadAddressResponseTest {

    @Test
    @DisplayName("주소 조회 결과를 API 응답으로 변환한다")
    void success_From() {
        // given
        RoadAddressView view = new RoadAddressView(
                "광주광역시 북구 비엔날레로 111 (용봉동)",
                "광주광역시 북구 비엔날레로 111",
                " (용봉동)",
                "광주광역시 북구 용봉동 1",
                "61104",
                "광주비엔날레 전시관",
                "2917011200100010000000001"
        );

        // when
        RoadAddressResponse response = RoadAddressResponse.from(view);

        // then
        assertThat(response.roadAddress()).isEqualTo(view.roadAddress());
        assertThat(response.buildingManagementNumber())
                .isEqualTo(view.buildingManagementNumber());
    }
}
