package com.example.chookjibupadmin.auth.command.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.example.chookjibupadmin.admin.command.domain.vo.AdminEmail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SmtpAdminEmailVerificationSenderTest {

    @InjectMocks
    private SmtpAdminEmailVerificationSender sender;

    @Mock
    private JavaMailSender javaMailSender;

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        @DisplayName("SMTP 메일로 관리자 이메일 인증 코드를 발송한다")
        void success_Send() {
            // given
            ReflectionTestUtils.setField(
                    sender,
                    "from",
                    "sender@gmail.com"
            );
            AdminEmail email = AdminEmail.of("admin@mapo.go.kr");

            // when
            sender.send(email, "123456");

            // then
            ArgumentCaptor<SimpleMailMessage> captor =
                    ArgumentCaptor.forClass(SimpleMailMessage.class);
            then(javaMailSender).should().send(captor.capture());
            SimpleMailMessage message = captor.getValue();

            assertThat(message.getFrom()).isEqualTo("sender@gmail.com");
            assertThat(message.getTo()).containsExactly("admin@mapo.go.kr");
            assertThat(message.getText()).contains("123456");
        }
    }
}
