package com.example.chookjibupadmin.auth.command.infrastructure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * Gmail이 네 글자 단위로 표시하는 앱 비밀번호의 공백을 제거한다.
 */
@Component
public class GmailAppPasswordNormalizer implements BeanPostProcessor {

    private static final String GMAIL_SMTP_HOST = "smtp.gmail.com";

    @Override
    public Object postProcessAfterInitialization(
            Object bean,
            String beanName
    ) throws BeansException {
        if (bean instanceof JavaMailSenderImpl mailSender
                && isGmailSmtp(mailSender)) {
            mailSender.setPassword(removeWhitespace(mailSender.getPassword()));
        }
        return bean;
    }

    private boolean isGmailSmtp(JavaMailSenderImpl mailSender) {
        return GMAIL_SMTP_HOST.equalsIgnoreCase(mailSender.getHost());
    }

    private String removeWhitespace(String password) {
        if (password == null) {
            return null;
        }

        StringBuilder normalized = new StringBuilder(password.length());
        password.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .forEach(normalized::appendCodePoint);
        return normalized.toString();
    }
}
