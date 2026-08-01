package com.example.chookjibupadmin.auth.command.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class GmailAppPasswordNormalizerTest {

    private final GmailAppPasswordNormalizer normalizer =
            new GmailAppPasswordNormalizer();

    @Nested
    @DisplayName("postProcessAfterInitialization")
    class PostProcessAfterInitialization {

        @Test
        @DisplayName("Gmail 앱 비밀번호의 모든 공백을 제거한다")
        void success_PostProcessAfterInitialization_GmailPassword() {
            // given
            JavaMailSenderImpl mailSender = mailSender(
                    "smtp.gmail.com",
                    "abcd efgh\tijkl\nmnop"
            );

            // when
            Object result = normalizer.postProcessAfterInitialization(
                    mailSender,
                    "mailSender"
            );

            // then
            assertThat(result).isSameAs(mailSender);
            assertThat(mailSender.getPassword()).isEqualTo("abcdefghijklmnop");
        }

        @Test
        @DisplayName("Gmail SMTP 호스트는 대소문자와 관계없이 인식한다")
        void success_PostProcessAfterInitialization_UppercaseGmailHost() {
            // given
            JavaMailSenderImpl mailSender = mailSender(
                    "SMTP.GMAIL.COM",
                    "abcd efgh ijkl mnop"
            );

            // when
            normalizer.postProcessAfterInitialization(
                    mailSender,
                    "mailSender"
            );

            // then
            assertThat(mailSender.getPassword()).isEqualTo("abcdefghijklmnop");
        }

        @Test
        @DisplayName("다른 SMTP 서버의 비밀번호는 변경하지 않는다")
        void success_PostProcessAfterInitialization_OtherSmtpHost() {
            // given
            JavaMailSenderImpl mailSender = mailSender(
                    "smtp.example.com",
                    "password with spaces"
            );

            // when
            normalizer.postProcessAfterInitialization(
                    mailSender,
                    "mailSender"
            );

            // then
            assertThat(mailSender.getPassword())
                    .isEqualTo("password with spaces");
        }

        @Test
        @DisplayName("Gmail 비밀번호가 설정되지 않았으면 null을 유지한다")
        void success_PostProcessAfterInitialization_NullPassword() {
            // given
            JavaMailSenderImpl mailSender = mailSender(
                    "smtp.gmail.com",
                    null
            );

            // when
            normalizer.postProcessAfterInitialization(
                    mailSender,
                    "mailSender"
            );

            // then
            assertThat(mailSender.getPassword()).isNull();
        }
    }

    private JavaMailSenderImpl mailSender(
            String host,
            String password
    ) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPassword(password);
        return mailSender;
    }
}
