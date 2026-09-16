package com.example.chookjibupadmin.booth.command.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.chookjibupadmin.booth.command.application.port.QueuePlanRecommendationPort;
import com.example.chookjibupadmin.map.analysis.infrastructure.openai.MapAnalysisProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class QueueRecommendationConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(QueueRecommendationConfig.class, PropertiesConfig.class)
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({QueueRecommendationProperties.class, MapAnalysisProperties.class})
    static class PropertiesConfig {}

    @Test
    void success_Create_ApiKeyEnablesQueueWhileMapAnalysisDisabled() {
        runner.withPropertyValues("app.map.analysis.provider=disabled", "app.map.analysis.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasSingleBean(QueuePlanRecommendationPort.class);
                    assertThat(context.getBean(QueuePlanRecommendationPort.class)).isInstanceOf(OpenAiQueuePlanRecommendationAdapter.class);
                    assertThat(context.getBean(QueuePlanRecommendationPort.class).unavailableReason()).isNull();
                });
    }

    @Test
    void success_Create_MissingKeyReportsUnavailableWithoutBreakingStartup() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(QueuePlanRecommendationPort.class).unavailableReason()).contains("APP_OPENAI_API_KEY");
        });
        runner.withPropertyValues("app.booth.queue-recommendation.provider=openai").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(QueuePlanRecommendationPort.class).unavailableReason()).contains("APP_OPENAI_API_KEY");
        });
    }

    @Test
    void success_Create_ExplicitDisabledHonoredWithApiKey() {
        runner.withPropertyValues("app.booth.queue-recommendation.provider=disabled", "app.map.analysis.api-key=test-key")
                .run(context -> assertThat(context.getBean(QueuePlanRecommendationPort.class).unavailableReason()).contains("비활성화"));
    }

    @Test
    void fail_Create_UnknownProvider() {
        runner.withPropertyValues("app.booth.queue-recommendation.provider=unknown")
                .run(context -> assertThat(context).hasFailed());
    }
}
