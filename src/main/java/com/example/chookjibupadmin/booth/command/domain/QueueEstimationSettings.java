package com.example.chookjibupadmin.booth.command.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 부스 전체 처리속도와 대기자 간격. 창구 수를 중복 적용하지 않는다. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QueueEstimationSettings {
    private double metersPerPerson;
    private double servedPersonsPerMinute;

    public static QueueEstimationSettings of(double metersPerPerson, double servedPersonsPerMinute) {
        if (!Double.isFinite(metersPerPerson) || metersPerPerson < 0.2 || metersPerPerson > 5
                || !Double.isFinite(servedPersonsPerMinute)
                || servedPersonsPerMinute < 0.1 || servedPersonsPerMinute > 100) {
            throw new IllegalArgumentException();
        }
        QueueEstimationSettings settings = new QueueEstimationSettings();
        settings.metersPerPerson = metersPerPerson;
        settings.servedPersonsPerMinute = servedPersonsPerMinute;
        return settings;
    }

    public static QueueEstimationSettings defaults() { return of(1, 2); }

    public BoothCongestionEstimate estimate(double meters) {
        if (!Double.isFinite(meters) || meters < 0 || meters > QueueGeometry.MAX_LENGTH_METERS) {
            throw new IllegalArgumentException();
        }
        int minutes = meters == 0 ? 0 : (int) Math.max(5,
                Math.round(meters / metersPerPerson / servedPersonsPerMinute / 5) * 5);
        return new BoothCongestionEstimate(minutes <= 10 ? BoothCongestionLevel.LOW
                : minutes <= 30 ? BoothCongestionLevel.MEDIUM : BoothCongestionLevel.HIGH, minutes);
    }
}
