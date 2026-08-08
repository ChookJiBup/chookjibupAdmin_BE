package com.example.chookjibupadmin.auth.command.infrastructure;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.auth.command.application.port.AdminPasswordResetEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 로컬 환경에서 비밀번호 재설정 링크를 로그로 확인하게 한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.email.verification.sender",
        havingValue = "log",
        matchIfMissing = true
)
public class LoggingAdminPasswordResetEmailSender
        implements AdminPasswordResetEmailSender {

    @Override
    public void send(AdminEmail email, String resetUrl) {
        log.info(
                "Admin password reset email requested. email={}, resetUrl={}",
                email.getValue(),
                resetUrl
        );
    }
}
