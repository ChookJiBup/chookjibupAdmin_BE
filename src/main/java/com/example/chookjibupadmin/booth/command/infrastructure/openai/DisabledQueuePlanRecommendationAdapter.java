package com.example.chookjibupadmin.booth.command.infrastructure.openai;

import com.example.chookjibupadmin.booth.command.application.port.QueuePlanRecommendationPort;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;

/** AI 설정이 없는 환경은 명확히 비활성 응답하며 수동 설정은 계속 지원한다. */
public class DisabledQueuePlanRecommendationAdapter implements QueuePlanRecommendationPort {
    private final String reason;
    public DisabledQueuePlanRecommendationAdapter() {
        this("서버에서 AI 줄 추천이 비활성화되어 있습니다.");
    }
    public DisabledQueuePlanRecommendationAdapter(String reason) { this.reason = reason; }
    @Override
    public String unavailableReason() { return reason; }
    public Proposal recommend(Input input) {
        throw new CustomException(ErrorCode.BOOTH_QUEUE_RECOMMENDATION_UNAVAILABLE);
    }
}
