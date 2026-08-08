package com.example.chookjibupadmin.auth.command.application.port;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import java.time.Duration;

/**
 * 관리자 인증 메일 요청 횟수와 재요청 간격을 제한한다.
 */
public interface AdminAuthRequestLimiter {

    /**
     * 허용된 요청이면 제한 상태를 기록하고, 제한을 넘으면 false를 반환한다.
     */
    boolean tryAcquire(
            String action,
            AdminEmail email,
            int requestLimit,
            Duration requestWindow,
            Duration resendCooldown
    );
}
