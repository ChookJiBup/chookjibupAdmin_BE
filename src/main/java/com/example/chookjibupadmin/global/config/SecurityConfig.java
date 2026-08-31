package com.example.chookjibupadmin.global.config;

import com.example.chookjibupadmin.auth.support.JwtAuthenticationFilter;
import com.example.chookjibupadmin.global.security.ApiAccessDeniedHandler;
import com.example.chookjibupadmin.global.security.ApiAuthenticationEntryPoint;
import com.example.chookjibupadmin.operator.support.FieldStaffAuthenticationFilter;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 관리자·현장 스태프·내부 API의 Stateless 보안 설정을 구성한다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final FieldStaffAuthenticationFilter fieldStaffAuthenticationFilter;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;

    /**
     * 관리자 JWT 필터가 Security 체인 밖에서 중복 실행되지 않도록 한다.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter>
            adminJwtFilterRegistration(JwtAuthenticationFilter filter) {
        return disabledRegistration(filter);
    }

    /**
     * 현장 스태프 JWT 필터가 Security 체인 밖에서 중복 실행되지 않도록 한다.
     */
    @Bean
    public FilterRegistrationBean<FieldStaffAuthenticationFilter>
            fieldStaffJwtFilterRegistration(
                    FieldStaffAuthenticationFilter filter
            ) {
        return disabledRegistration(filter);
    }

    /**
     * 회원가입과 로그인은 공개하고 그 외 관리자 API는 인증을 요구한다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/admin/auth/signup").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/admin/auth/signup/contractor"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admin/auth/logout").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/admin/auth/password-reset/request",
                                "/api/admin/auth/password-reset/confirm"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/admin/auth/email-verification/request"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/admin/auth/email-verification/confirm"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/field-staff/auth/login",
                                "/api/field-staff/auth/logout"
                        ).permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/internal/api/**").permitAll()
                        .requestMatchers("/api/field-staff/**")
                        .hasRole("FIELD_STAFF")
                        .requestMatchers(
                                "/api/festivals/*/operations/visitors",
                                "/api/festivals/*/operations/visitors/**"
                        )
                        .hasRole("ADMIN")
                        .requestMatchers(
                                "/api/festivals/*/operations",
                                "/api/festivals/*/operations/**"
                        )
                        .hasAnyRole("ADMIN", "FIELD_STAFF")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/festivals/*/dashboard"
                        )
                        .hasAnyRole("ADMIN", "FIELD_STAFF")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/festivals/*/booths/*/congestion"
                        )
                        .hasAnyRole("ADMIN", "FIELD_STAFF")
                        .anyRequest().hasRole("ADMIN")
                )
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterAfter(
                        fieldStaffAuthenticationFilter,
                        JwtAuthenticationFilter.class
                )
                .build();
    }

    /**
     * 관리자 비밀번호 저장에 사용할 단방향 해시 인코더를 제공한다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private <T extends Filter> FilterRegistrationBean<T> disabledRegistration(
            T filter
    ) {
        FilterRegistrationBean<T> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
