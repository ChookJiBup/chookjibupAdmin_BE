package com.example.chookjibupadmin.auth.command.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email.verification")
public record AdminEmailProperties(
        String sender,
        String fromAddress,
        String fromName,
        String replyTo
) {

    public boolean hasReplyTo() {
        return replyTo != null && !replyTo.isBlank();
    }
}
