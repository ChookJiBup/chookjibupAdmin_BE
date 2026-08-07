package com.example.chookjibupadmin.auth.command.infrastructure;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.auth.command.application.port.AdminEmailVerificationSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    private final AdminEmailProperties properties;

    @Override
    public void send(AdminEmail email, String code) {
        MimeMessage message = javaMailSender.createMimeMessage();
        prepare(message, email, code);
        javaMailSender.send(message);
    }

    private void prepare(
            MimeMessage message,
            AdminEmail email,
            String code
    ) {
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(
                    properties.fromAddress(),
                    properties.fromName()
            );
            helper.setTo(email.getValue());
            if (properties.hasReplyTo()) {
                helper.setReplyTo(properties.replyTo());
            }
            helper.setSubject("[축지법] 관리자 이메일 인증 코드");
            helper.setText("""
                    관리자 회원가입 이메일 인증 코드입니다.

                    인증 코드: %s

                    인증 코드는 5분 동안 유효합니다.
                    """.formatted(code));
        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new MailPreparationException(exception);
        }
    }
}
