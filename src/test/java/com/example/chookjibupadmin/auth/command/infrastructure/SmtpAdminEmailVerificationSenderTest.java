package com.example.chookjibupadmin.auth.command.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpAdminEmailVerificationSenderTest {

    private SmtpAdminEmailVerificationSender sender;

    @Mock
    private JavaMailSender javaMailSender;

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        @DisplayName("SMTP 메일로 관리자 이메일 인증 코드를 발송한다")
        void success_Send() throws Exception {
            // given
            AdminEmailProperties properties = new AdminEmailProperties(
                    "smtp",
                    "no-reply@festival.example",
                    "축지법",
                    "support@festival.example"
            );
            sender = new SmtpAdminEmailVerificationSender(
                    javaMailSender,
                    properties
            );
            MimeMessage message = new MimeMessage(
                    Session.getInstance(new Properties())
            );
            given(javaMailSender.createMimeMessage()).willReturn(message);
            AdminEmail email = AdminEmail.of("admin@mapo.go.kr");

            // when
            sender.send(email, "123456");

            // then
            ArgumentCaptor<MimeMessage> captor =
                    ArgumentCaptor.forClass(MimeMessage.class);
            then(javaMailSender).should().send(captor.capture());
            MimeMessage sentMessage = captor.getValue();
            InternetAddress from = (InternetAddress) sentMessage.getFrom()[0];
            InternetAddress replyTo =
                    (InternetAddress) sentMessage.getReplyTo()[0];

            assertThat(from.getAddress())
                    .isEqualTo("no-reply@festival.example");
            assertThat(from.getPersonal()).isEqualTo("축지법");
            assertThat(replyTo.getAddress())
                    .isEqualTo("support@festival.example");
            assertThat(sentMessage.getAllRecipients()[0].toString())
                    .isEqualTo("admin@mapo.go.kr");
            assertThat(sentMessage.getSubject())
                    .isEqualTo("[축지법] 관리자 이메일 인증 코드");
            assertThat(sentMessage.getContent().toString()).contains("123456");
        }
    }
}
