package com.example.chookjibupadmin.booth.command.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BoothCongestionEstimatorTest {

    private final BoothCongestionEstimator estimator = new BoothCongestionEstimator();

    @Test
    @DisplayName("줄끝 거리가 없으면 혼잡도를 계산하지 않는다")
    void success_Estimate_Null() {
        assertThat(estimator.estimate(null)).isEmpty();
    }

    @Test
    @DisplayName("0미터는 여유와 0분으로 계산한다")
    void success_Estimate_Zero() {
        assertThat(estimator.estimate(0))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.LOW, 0));
    }

    @Test
    @DisplayName("10미터는 여유와 5분으로 계산한다")
    void success_Estimate_RoundingUnit() {
        assertThat(estimator.estimate(10))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.LOW, 5));
    }

    @Test
    @DisplayName("12미터와 18미터는 서로 다른 대기시간으로 계산한다")
    void success_Estimate_DistinguishesNearbyDistances() {
        assertThat(estimator.estimate(12))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.LOW, 5));
        assertThat(estimator.estimate(18))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.LOW, 10));
    }

    @Test
    @DisplayName("줄이 조금이라도 있으면 최소 5분으로 안내한다")
    void success_Estimate_ShortDistance() {
        assertThat(estimator.estimate(1))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.LOW, 5));
    }

    @Test
    @DisplayName("여유 상한인 24미터는 여유와 10분으로 계산한다")
    void success_Estimate_LowMaxBoundary() {
        assertThat(estimator.estimate(24))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.LOW, 10));
    }

    @Test
    @DisplayName("25미터는 보통과 15분으로 계산한다")
    void success_Estimate_MediumBoundary() {
        assertThat(estimator.estimate(25))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.MEDIUM, 15));
    }

    @Test
    @DisplayName("보통 상한인 64미터는 보통과 30분으로 계산한다")
    void success_Estimate_MediumMaxBoundary() {
        assertThat(estimator.estimate(64))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.MEDIUM, 30));
    }

    @Test
    @DisplayName("65미터는 혼잡과 35분으로 계산한다")
    void success_Estimate_HighBoundary() {
        assertThat(estimator.estimate(65))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.HIGH, 35));
    }

    @Test
    @DisplayName("실제 축제 좌표에서 나온 16미터는 여유와 10분으로 계산한다")
    void success_Estimate_RealShortQueue() {
        assertThat(estimator.estimate(16))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.LOW, 10));
    }

    @Test
    @DisplayName("실제 축제 좌표에서 나온 133미터는 혼잡과 65분으로 계산한다")
    void success_Estimate_RealLongQueue() {
        assertThat(estimator.estimate(133))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.HIGH, 65));
    }

    @Test
    @DisplayName("음수 줄끝 거리는 거절한다")
    void fail_Estimate_Negative_CustomException() {
        assertThatThrownBy(() -> estimator.estimate(-1))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
    }

    @Test
    @DisplayName("정수 최대 거리도 정수 범위 안의 대기시간으로 계산한다")
    void success_Estimate_MaxMeters() {
        assertThat(estimator.estimate(Integer.MAX_VALUE))
                .hasValueSatisfying(estimate -> {
                    assertThat(estimate.congestionLevel())
                            .isEqualTo(BoothCongestionLevel.HIGH);
                    assertThat(estimate.waitMinutes()).isPositive();
                });
    }
}
