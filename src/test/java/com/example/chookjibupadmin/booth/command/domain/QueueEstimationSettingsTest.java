package com.example.chookjibupadmin.booth.command.domain;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class QueueEstimationSettingsTest {
    @Test void success_Estimate_DefaultAndEmpty() {
        assertThat(QueueEstimationSettings.defaults().estimate(18).waitMinutes()).isEqualTo(10);
        assertThat(QueueEstimationSettings.defaults().estimate(12).waitMinutes()).isEqualTo(5);
        assertThat(QueueEstimationSettings.defaults().estimate(0).waitMinutes()).isZero();
        assertThat(QueueEstimationSettings.defaults().estimate(1).waitMinutes()).isEqualTo(5);
    }
    @Test void success_Estimate_BoothSpeedAndSpacing() {
        assertThat(QueueEstimationSettings.of(1,1).estimate(40).waitMinutes()).isEqualTo(40);
        assertThat(QueueEstimationSettings.of(2,2).estimate(40).waitMinutes()).isEqualTo(10);
    }
    @Test void fail_Of_InvalidSettings() {
        assertThatThrownBy(() -> QueueEstimationSettings.of(0,2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QueueEstimationSettings.of(1,Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QueueEstimationSettings.defaults().estimate(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
