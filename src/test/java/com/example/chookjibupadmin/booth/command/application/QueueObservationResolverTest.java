package com.example.chookjibupadmin.booth.command.application;

import static org.assertj.core.api.Assertions.*;
import com.example.chookjibupadmin.booth.command.application.dto.UpdateBoothQueueCommand;
import com.example.chookjibupadmin.booth.command.application.dto.UpdateBoothQueueCommand.QueuePathPointCommand;
import com.example.chookjibupadmin.booth.command.domain.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QueueObservationResolverTest {
    @Test void success_Resolve_PathDeletionKeepsOriginalObservationTime() {
        var queue=BoothQueue.createEmpty(1L,2L);
        queue.updateTail(new BigDecimal("37.00018"),new BigDecimal("127"),20,List.of(p("37"),p("37.00018")),
                BoothQueueModifierType.ADMIN,3L,null);
        queue.recordObservation(QueueEstimationSettings.defaults().estimate(20),java.time.LocalDateTime.of(2026,9,16,9,0),null,"PATH");
        var result=QueueObservationResolver.resolve(queue,null,p("37"),command("37.00018",null,List.of()));
        assertThat(result.path()).isNull();
        assertThat(result.estimate().waitMinutes()).isEqualTo(10);
        assertThat(result.refreshObservation()).isFalse();
    }
    private Map<String,BigDecimal> p(String lat) { return QueueGeometry.point(new BigDecimal(lat),new BigDecimal("127")); }
    private UpdateBoothQueueCommand command(String lat,Integer meters,List<QueuePathPointCommand> path) {
        return new UpdateBoothQueueCommand(new BigDecimal(lat),new BigDecimal("127"),meters,path);
    }
    @Test void success_Resolve_PathComputesWithoutReportedMeters() {
        var path=List.of(new QueuePathPointCommand(new BigDecimal("37"),new BigDecimal("127")),
                new QueuePathPointCommand(new BigDecimal("37.00018"),new BigDecimal("127")));
        var observation=QueueObservationResolver.resolve(BoothQueue.createEmpty(1L,2L),null,p("37"),command("37.00018",999,path));
        assertThat(observation.meters()).isEqualTo(20);
        assertThat(observation.estimate().waitMinutes()).isEqualTo(10);
        assertThat(observation.method()).isEqualTo("PATH");
    }
    @Test void success_Resolve_PartialPlanAndZero() {
        var plan=BoothQueuePlan.create(1L,2L);
        plan.replace(List.of(p("37"),p("37.0004")),QueueEstimationSettings.defaults(),null,3L,0);
        var observation=QueueObservationResolver.resolve(BoothQueue.createEmpty(1L,2L),plan,p("37"),command("37.00018",null,null));
        assertThat(observation.estimate().waitMinutes()).isEqualTo(10);
        assertThat(observation.planRevision()).isEqualTo(1L);
        var empty=QueueObservationResolver.resolve(BoothQueue.createEmpty(1L,2L),plan,p("37"),command("37",0,null));
        assertThat(empty.estimate().waitMinutes()).isZero();
        assertThat(empty.path()).isNull();
    }
    @Test void success_Resolve_UnknownAndStraightFallback() {
        var unknown=QueueObservationResolver.resolve(BoothQueue.createEmpty(1L,2L),null,null,command("37.00018",null,null));
        assertThat(unknown.estimate()).isNull();
        var straight=QueueObservationResolver.resolve(BoothQueue.createEmpty(1L,2L),null,p("37"),command("37.00018",999,null));
        assertThat(straight.meters()).isEqualTo(20);
    }
    @Test void fail_Resolve_StalePlanRevision() {
        assertThatThrownBy(() -> QueueObservationResolver.resolve(BoothQueue.createEmpty(1L,2L),null,p("37"),
                new UpdateBoothQueueCommand(new BigDecimal("37"),new BigDecimal("127"),null,null,0L,1L)))
                .isInstanceOf(RuntimeException.class);
    }
}
