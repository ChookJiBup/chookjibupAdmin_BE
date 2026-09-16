package com.example.chookjibupadmin.booth.command.domain;

import static org.assertj.core.api.Assertions.*;
import static com.example.chookjibupadmin.booth.command.domain.QueueGeometryTest.p;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueuePlanConstraintsTest {
    @Test void fail_Validate_AdjacentBacktracking() {
        assertThatThrownBy(() -> QueueGeometry.validate(List.of(p(37,127),p(37.0003,127),p(37.0001,127))))
                .isInstanceOf(com.example.chookjibupadmin.global.response.CustomException.class);
    }
    @Test void success_Validate_ClosedTriangleWithoutOverlap() {
        QueueGeometry.validate(List.of(p(37,127),p(37.0003,127),p(37.0003,127.0003),p(37,127)));
    }
    @Test void fail_Validate_CollinearQueueOverlap() {
        assertThatThrownBy(() -> QueuePlanConstraints.validate(List.of(p(37,127),p(37.0004,127)),null,
                List.of(new QueuePlanConstraints.Obstacle(List.of(p(37.0001,127),p(37.0003,127)),false))))
                .isInstanceOf(RuntimeException.class);
    }
    @Test void fail_Validate_SelfIntersection() {
        assertThatThrownBy(() -> QueueGeometry.validate(List.of(p(37,127),p(37.0002,127.0002),
                p(37,127.0002),p(37.0002,127)))).isInstanceOf(RuntimeException.class);
    }
    @Test void success_Validate_InsideBoundary() {
        QueuePlanConstraints.validate(List.of(p(37.0001,127.0001),p(37.0002,127.0002)),
                List.of(p(37,127),p(37,127.001),p(37.001,127.001),p(37.001,127)),List.of());
    }
    @Test void fail_Validate_CrossingAnotherQueue() {
        assertThatThrownBy(() -> QueuePlanConstraints.validate(List.of(p(37,127),p(37.0002,127.0002)),null,
                List.of(new QueuePlanConstraints.Obstacle(List.of(p(37,127.0002),p(37.0002,127)),false))))
                .isInstanceOf(RuntimeException.class);
    }
    @Test void fail_Validate_OutsideBoundaryAndFacility() {
        var square=List.of(p(37,127),p(37,127.001),p(37.001,127.001),p(37.001,127));
        assertThatThrownBy(() -> QueuePlanConstraints.validate(List.of(p(37.002,127),p(37.003,127)),square,List.of()))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> QueuePlanConstraints.validate(List.of(p(37.0001,127.0001),p(37.0002,127.0002)),null,
                List.of(new QueuePlanConstraints.Obstacle(square,true)))).isInstanceOf(RuntimeException.class);
    }
}
