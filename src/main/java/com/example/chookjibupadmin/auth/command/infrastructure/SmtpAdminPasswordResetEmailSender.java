package com.example.chookjibupadmin.auth.command.infrastructure;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import com.example.chookjibupadmin.auth.command.application.port.AdminPasswordResetEmailSender;
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
 * SMTP 서버를 통해 관리자 비밀번호 재설정 링크를 발송한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.email.verification.sender",
        havingValue = "smtp"
)
public class SmtpAdminPasswordResetEmailSender
        implements AdminPasswordResetEmailSender {

    private final JavaMailSender javaMailSender;
    private final AdminEmailProperties properties;

    @Override
    public void send(AdminEmail email, String resetUrl) {
        MimeMessage message = javaMailSender.createMimeMessage();
        prepare(message, email, resetUrl);
        javaMailSender.send(message);
    }

    private void prepare(
            MimeMessage message,
            AdminEmail email,
            String resetUrl
    ) {
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(properties.fromAddress(), properties.fromName());
            helper.setTo(email.getValue());
            if (properties.hasReplyTo()) {
                helper.setReplyTo(properties.replyTo());
            }
            helper.setSubject("[축지법] 관리자 비밀번호 재설정");
            helper.setText("""
                    관리자 비밀번호 재설정 요청이 접수되었습니다.

                    아래 링크에서 새 비밀번호를 설정해 주세요.
                    %s

                    링크는 설정된 유효 시간 동안 한 번만 사용할 수 있습니다.
                    본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                    """.formatted(resetUrl));
        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new MailPreparationException(exception);
        }
    }
}
