package com.example.chookjibupadmin.auth.command.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@SpringBootTest(properties = {
        "spring.mail.host=smtp.gmail.com",
        "spring.mail.username=sender@gmail.com",
        "spring.mail.password=abcd efgh ijkl mnop"
})
class GmailAppPasswordNormalizerIntegrationTest {

    @Autowired
    private JavaMailSender javaMailSender;

    @Test
    @DisplayName("Spring Mail 발송기에 공백이 제거된 Gmail 앱 비밀번호가 설정된다")
    void success_Normalize_SpringMailSender() {
        // given
        JavaMailSenderImpl mailSender =
                (JavaMailSenderImpl) javaMailSender;

        // when
        String password = mailSender.getPassword();

        // then
        assertThat(password).isEqualTo("abcdefghijklmnop");
    }
}
