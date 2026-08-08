package com.example.chookjibupadmin.global.config;

import com.example.chookjibupadmin.dashboard.query.application.port.FestivalDashboardMetricProvider;
import com.example.chookjibupadmin.dashboard.query.infrastructure.UnavailableFestivalDashboardMetricProvider;
import com.example.chookjibupadmin.report.query.application.port.FestivalReportMetricProvider;
import com.example.chookjibupadmin.report.query.infrastructure.UnavailableFestivalReportMetricProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 운영 지표 연동 구현이 없을 때 데이터 미연결 대체 구현을 제공한다.
 */
@Configuration(proxyBeanMethods = false)
public class FestivalMetricProviderConfig {

    @Bean
    @ConditionalOnMissingBean(FestivalDashboardMetricProvider.class)
    public FestivalDashboardMetricProvider festivalDashboardMetricProvider() {
        return new UnavailableFestivalDashboardMetricProvider();
    }

    @Bean
    @ConditionalOnMissingBean(FestivalReportMetricProvider.class)
    public FestivalReportMetricProvider festivalReportMetricProvider() {
        return new UnavailableFestivalReportMetricProvider();
    }
}
