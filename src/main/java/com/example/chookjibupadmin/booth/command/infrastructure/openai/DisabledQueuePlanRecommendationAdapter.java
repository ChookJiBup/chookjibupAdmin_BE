package com.example.chookjibupadmin.booth.command.infrastructure.openai;

import com.example.chookjibupadmin.booth.command.application.port.QueuePlanRecommendationPort;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** AI 설정이 없는 환경은 명확히 비활성 응답하며 수동 설정은 계속 지원한다. */
@Component
@ConditionalOnProperty(prefix = "app.map.analysis", name = "provider", havingValue = "disabled", matchIfMissing = true)
public class DisabledQueuePlanRecommendationAdapter implements QueuePlanRecommendationPort {
    public Proposal recommend(Input input) {
        throw new CustomException(ErrorCode.BOOTH_QUEUE_RECOMMENDATION_UNAVAILABLE);
    }
}
