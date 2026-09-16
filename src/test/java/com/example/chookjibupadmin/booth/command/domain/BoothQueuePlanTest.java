package com.example.chookjibupadmin.booth.command.domain;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class BoothQueuePlanTest {
    @Test void success_Replace_PlanDoesNotCreateObservation() {
        var plan = BoothQueuePlan.create(1L,2L);
        plan.replace(List.of(QueueGeometryTest.p(37,127),QueueGeometryTest.p(37.0004,127)),
                QueueEstimationSettings.defaults(),null,3L,0);
        assertThat(plan.getRevision()).isEqualTo(1);
        assertThat(plan.getLengthMeters()).isBetween(44.0,45.0);
        assertThat(BoothQueue.createEmpty(1L,2L).getWaitMinutes()).isNull();
        assertThatThrownBy(() -> plan.replace(plan.getPathGeometry(),plan.getSettings(),null,3L,0))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> plan.getPathGeometry().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
