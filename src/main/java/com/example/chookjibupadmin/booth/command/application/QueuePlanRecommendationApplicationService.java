package com.example.chookjibupadmin.booth.command.application;

import com.example.chookjibupadmin.auth.support.FestivalActorPrincipal;
import com.example.chookjibupadmin.booth.command.application.port.QueuePlanRecommendationPort;
import com.example.chookjibupadmin.booth.command.domain.QueueGeometry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 동기식 미확정 추천이다. 저장 시 최신 제약을 다시 검증한다. */
@Service
@RequiredArgsConstructor
public class QueuePlanRecommendationApplicationService {
    private final QueueRecommendationContextService contextService;
    private final QueuePlanRecommendationPort port;
    public record Recommendation(List<Map<String, BigDecimal>> path, String reason,
            double lengthMeters, Long expectedNodeVersion, long expectedRevision, List<String> warnings) {}
    public Recommendation recommend(UUID festivalId, Long boothId, int capacity, double spacing,
            FestivalActorPrincipal principal) {
        var context = contextService.read(festivalId, boothId, capacity, spacing, principal);
        var proposal = port.recommend(context.input());
        contextService.validate(festivalId, boothId, context, proposal.path(), principal);
        return new Recommendation(proposal.path(), proposal.reason(), QueueGeometry.length(proposal.path()),
                context.nodeVersion(), context.planRevision(),
                List.of("시설의 실제 크기와 현장 통행 여유를 확인한 뒤 적용해 주세요."));
    }
}
