package com.example.chookjibupadmin.booth.command.domain;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.util.Optional;

/**
 * 줄끝 거리를 운영 참고용 혼잡도와 대기시간으로 환산한다.
 *
 * <p>환산 근거는 다음 두 가지 현장 가정이다.</p>
 * <ul>
 *   <li>대기자 1명이 차지하는 줄 길이 1m.
 *       한 줄로 설 때 몸 두께와 앞사람과의 간격을 합친 값이다.
 *       즉 줄 길이 1m는 대기자 약 1명이다.</li>
 *   <li>부스 1곳의 처리 속도 분당 2명.
 *       축제 부스는 보통 창구 2곳이 동시에 응대하고,
 *       창구 하나가 주문·수령까지 1명을 약 60초에 처리한다고 본다.</li>
 * </ul>
 *
 * <p>두 가정을 곱하면 대기자 1명당 0.5분이므로
 * 대기시간(분)은 줄끝 거리(m)의 절반이다.
 * 스태프가 지도에서 찍은 좌표 사이의 직선거리라 오차가 크므로
 * 분 단위까지 보여 주지 않고 {@code 10m = 5분} 단위로 반올림한다.
 * 올림이 아니라 반올림이라야 12m와 18m처럼 다른 거리가 같은 값으로 뭉개지지 않는다
 * (12m는 5분, 18m는 10분).
 * 줄이 없으면(0m) 0분이고, 줄이 조금이라도 있으면 최소 5분으로 안내한다.</p>
 *
 * <p>혼잡 등급은 거리가 아니라 환산한 대기시간으로 나눈다.
 * 방문객이 체감하는 기준이 거리가 아니라 기다리는 시간이기 때문이다.
 * 10분 이하는 여유(24m 이하), 30분 이하는 보통(64m 이하),
 * 그보다 오래 기다려야 하면 혼잡으로 본다.</p>
 */
public class BoothCongestionEstimator {

    /** 대기자 1명이 차지하는 줄 길이(m). */
    private static final long QUEUE_METERS_PER_PERSON = 1L;

    /** 부스 1곳이 1분에 처리하는 대기자 수(명). */
    private static final long SERVED_PERSONS_PER_MINUTE = 2L;

    /** 안내용 대기시간 표시 단위(분). */
    private static final long ROUNDING_UNIT_MINUTES = 5L;

    /**
     * 표시 단위 한 칸에 해당하는 줄 길이(m).
     * {@code 1m/명 × 2명/분 × 5분 = 10m}이다.
     */
    private static final long METERS_PER_ROUNDING_UNIT =
            QUEUE_METERS_PER_PERSON * SERVED_PERSONS_PER_MINUTE * ROUNDING_UNIT_MINUTES;

    /** 여유로 보는 대기시간 상한(분). */
    private static final int LOW_MAX_WAIT_MINUTES = 10;

    /** 보통으로 보는 대기시간 상한(분). */
    private static final int MEDIUM_MAX_WAIT_MINUTES = 30;

    /**
     * 줄끝 거리가 없으면 계산하지 않고, 있으면 5분 단위로 추정한다.
     */
    public Optional<BoothCongestionEstimate> estimate(Integer queueTailMeters) {
        if (queueTailMeters == null) {
            return Optional.empty();
        }
        if (queueTailMeters < 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        long roundedWaitMinutes = ((long) queueTailMeters + METERS_PER_ROUNDING_UNIT / 2)
                / METERS_PER_ROUNDING_UNIT
                * ROUNDING_UNIT_MINUTES;
        if (queueTailMeters > 0 && roundedWaitMinutes < ROUNDING_UNIT_MINUTES) {
            roundedWaitMinutes = ROUNDING_UNIT_MINUTES;
        }
        if (roundedWaitMinutes > Integer.MAX_VALUE) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        int waitMinutes = (int) roundedWaitMinutes;
        return Optional.of(new BoothCongestionEstimate(
                resolveLevel(waitMinutes),
                waitMinutes
        ));
    }

    private BoothCongestionLevel resolveLevel(int waitMinutes) {
        if (waitMinutes <= LOW_MAX_WAIT_MINUTES) {
            return BoothCongestionLevel.LOW;
        }
        if (waitMinutes <= MEDIUM_MAX_WAIT_MINUTES) {
            return BoothCongestionLevel.MEDIUM;
        }
        return BoothCongestionLevel.HIGH;
    }
}
