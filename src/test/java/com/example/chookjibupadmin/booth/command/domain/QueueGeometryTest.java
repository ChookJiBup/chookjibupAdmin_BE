package com.example.chookjibupadmin.booth.command.domain;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QueueGeometryTest {
    static Map<String, BigDecimal> p(double lat, double lng) {
        return QueueGeometry.point(BigDecimal.valueOf(lat), BigDecimal.valueOf(lng));
    }
    @Test void success_Length_BentPathLongerThanStraight() {
        var a=p(37,127); var b=p(37.0002,127); var c=p(37.0002,127.0002);
        assertThat(QueueGeometry.length(List.of(a,b,c))).isBetween(39.0,41.0);
        assertThat(QueueGeometry.length(List.of(a,b,c))).isGreaterThan(QueueGeometry.distance(a,c));
    }
    @Test void success_Length_ReturnToStartHasPositiveLength() {
        var a=p(37,127); var b=p(37.0002,127);
        assertThat(QueueGeometry.length(List.of(a,b,a))).isBetween(44.0,45.0);
    }
    @Test void success_OccupiedPath_PartialPlan() {
        var path=QueueGeometry.occupiedPath(List.of(p(37,127),p(37.0004,127)),p(37.00018,127));
        assertThat(QueueGeometry.length(path)).isBetween(19.0,21.0);
        assertThat(path.getLast().get("lat")).isEqualByComparingTo("37.00018");
    }
    @Test void success_OccupiedPath_StartIsEmpty() {
        assertThat(QueueGeometry.occupiedPath(List.of(p(37,127),p(37.0004,127)),p(37,127))).hasSize(1);
    }
    @Test void fail_OccupiedPath_OffPlan() {
        assertThatThrownBy(() -> QueueGeometry.occupiedPath(List.of(p(37,127),p(37.0004,127)),p(37,127.001)))
                .isInstanceOf(RuntimeException.class);
    }
    @Test void fail_Validate_DuplicateAndTooLong() {
        assertThatThrownBy(() -> QueueGeometry.validate(List.of(p(37,127),p(37,127)))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> QueueGeometry.validate(List.of(p(37,127),p(38,127)))).isInstanceOf(RuntimeException.class);
    }
    @Test void fail_Point_NormalizedCoordinates() {
        assertThatThrownBy(() -> p(0.5,0.5)).isInstanceOf(RuntimeException.class);
    }
}
