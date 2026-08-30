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
    @DisplayName("10미터 경계는 여유와 10분으로 계산한다")
    void success_Estimate_LowBoundary() {
        assertThat(estimator.estimate(10))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.LOW, 10));
    }

    @Test
    @DisplayName("11미터는 보통과 20분으로 계산한다")
    void success_Estimate_MediumBoundary() {
        assertThat(estimator.estimate(11))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.MEDIUM, 20));
    }

    @Test
    @DisplayName("30미터 경계는 보통과 30분으로 계산한다")
    void success_Estimate_MediumMaxBoundary() {
        assertThat(estimator.estimate(30))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.MEDIUM, 30));
    }

    @Test
    @DisplayName("31미터는 혼잡과 40분으로 계산한다")
    void success_Estimate_HighBoundary() {
        assertThat(estimator.estimate(31))
                .contains(new BoothCongestionEstimate(BoothCongestionLevel.HIGH, 40));
    }

    @Test
    @DisplayName("음수 줄끝 거리는 거절한다")
    void fail_Estimate_Negative_CustomException() {
        assertThatThrownBy(() -> estimator.estimate(-1))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
    }

    @Test
    @DisplayName("정수 범위를 넘는 추정 대기시간은 거절한다")
    void fail_Estimate_Overflow_CustomException() {
        assertThatThrownBy(() -> estimator.estimate(Integer.MAX_VALUE))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
    }
}
