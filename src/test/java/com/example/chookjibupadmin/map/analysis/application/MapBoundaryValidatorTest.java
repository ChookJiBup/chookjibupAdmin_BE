package com.example.chookjibupadmin.map.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.analysis.application.MapBoundaryValidator.BoundaryPoint;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapBoundaryValidatorTest {

    private final MapBoundaryValidator validator = new MapBoundaryValidator();

    @Test
    @DisplayName("유효한 삼각형 폴리곤을 통과한다")
    void success_ValidTriangle() {
        List<BoundaryPoint> validated = validator.validateClosedPolygon(List.of(
                point("37.5665", "126.9780"),
                point("37.5670", "126.9790"),
                point("37.5660", "126.9795"),
                point("37.5665", "126.9780")
        ));

        assertThat(validated).hasSize(3);
    }

    @Test
    @DisplayName("자기 교차 폴리곤은 거절한다")
    void fail_SelfIntersecting() {
        assertThatThrownBy(() -> validator.validateClosedPolygon(List.of(
                point("37.50", "126.90"),
                point("37.52", "126.92"),
                point("37.52", "126.90"),
                point("37.50", "126.92")
        )))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID.getMessage());
    }

    @Test
    @DisplayName("점이 너무 적으면 거절한다")
    void fail_TooFewPoints() {
        assertThatThrownBy(() -> validator.validateClosedPolygon(List.of(
                point("37.5665", "126.9780"),
                point("37.5670", "126.9790")
        )))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID.getMessage());
    }

    @Test
    @DisplayName("연속 중복 점은 거절한다")
    void fail_ConsecutiveDuplicate() {
        assertThatThrownBy(() -> validator.validateClosedPolygon(List.of(
                point("37.5665", "126.9780"),
                point("37.5665", "126.9780"),
                point("37.5670", "126.9790"),
                point("37.5660", "126.9795")
        )))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MAP_PRESENTATION_BOUNDARY_INVALID.getMessage());
    }

    private BoundaryPoint point(String lat, String lng) {
        return new BoundaryPoint(new BigDecimal(lat), new BigDecimal(lng));
    }
}
