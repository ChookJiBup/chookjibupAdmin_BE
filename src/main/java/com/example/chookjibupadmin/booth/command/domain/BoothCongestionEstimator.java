package com.example.chookjibupadmin.booth.command.domain;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.Optional;

/**
 * 줄끝 거리를 운영 참고용 혼잡도와 대기시간으로 환산한다.
 */
public class BoothCongestionEstimator {

    private static final long ROUNDING_UNIT_METERS = 10L;
    private static final int LOW_MAX_METERS = 10;
    private static final int MEDIUM_MAX_METERS = 30;

    /**
     * 줄끝 거리가 없으면 계산하지 않고, 있으면 10분 단위로 추정한다.
     */
    public Optional<BoothCongestionEstimate> estimate(Integer queueTailMeters) {
        if (queueTailMeters == null) {
            return Optional.empty();
        }
        if (queueTailMeters < 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        long roundedWaitMinutes = ((long) queueTailMeters + ROUNDING_UNIT_METERS - 1)
                / ROUNDING_UNIT_METERS
                * ROUNDING_UNIT_METERS;
        if (roundedWaitMinutes > Integer.MAX_VALUE) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        return Optional.of(new BoothCongestionEstimate(
                resolveLevel(queueTailMeters),
                (int) roundedWaitMinutes
        ));
    }

    private BoothCongestionLevel resolveLevel(int queueTailMeters) {
        if (queueTailMeters <= LOW_MAX_METERS) {
            return BoothCongestionLevel.LOW;
        }
        if (queueTailMeters <= MEDIUM_MAX_METERS) {
            return BoothCongestionLevel.MEDIUM;
        }
        return BoothCongestionLevel.HIGH;
    }
}
