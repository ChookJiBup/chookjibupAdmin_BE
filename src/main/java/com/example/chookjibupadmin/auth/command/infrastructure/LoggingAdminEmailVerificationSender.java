package com.example.chookjibupadmin.auth.command.infrastructure;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.auth.command.application.port.AdminEmailVerificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 실제 메일 연동 전까지 인증 코드를 로그로 남기는 임시 발송 구현체이다.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.email.verification.sender",
        havingValue = "log",
        matchIfMissing = true
)
public class LoggingAdminEmailVerificationSender
        implements AdminEmailVerificationSender {

    @Override
    public void send(AdminEmail email, String code) {
        log.info("Admin email verification code issued. email={}, code={}",
                email.getValue(),
                code
        );
    }
}
