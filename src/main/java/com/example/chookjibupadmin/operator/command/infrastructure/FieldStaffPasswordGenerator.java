package com.example.chookjibupadmin.operator.command.infrastructure;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * 현장 스태프 최초 로그인용 임시 비밀번호를 생성한다.
 */
@Component
public class FieldStaffPasswordGenerator {

    private static final int PASSWORD_LENGTH = 12;
    private static final char[] CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$".toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 관리자가 스태프에게 전달할 임시 비밀번호를 생성한다.
     */
    public String generate() {
        StringBuilder builder = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            builder.append(CHARACTERS[secureRandom.nextInt(CHARACTERS.length)]);
        }

        return builder.toString();
    }
}
