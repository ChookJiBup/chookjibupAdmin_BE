package com.example.chookjibupadmin.auth.command.infrastructure;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.auth.command.application.port.AdminEmailVerificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP 서버를 통해 관리자 이메일 인증 코드를 발송한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.email.verification.sender",
        havingValue = "smtp"
)
public class SmtpAdminEmailVerificationSender
        implements AdminEmailVerificationSender {

    private final JavaMailSender javaMailSender;

    @Value("${app.email.verification.from}")
    private String from;

    @Override
    public void send(AdminEmail email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email.getValue());
        message.setSubject("[Festival Flow] 관리자 이메일 인증 코드");
        message.setText("""
                관리자 회원가입 이메일 인증 코드입니다.

                인증 코드: %s

                인증 코드는 5분 동안 유효합니다.
                """.formatted(code));

        javaMailSender.send(message);
    }
}
