package com.example.demoadmin.booth.command.domain.vo;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoothCategory {

    private static final int MAX_LENGTH = 50;

    private String value;

    private BoothCategory(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        this.value = value;
    }

    public static BoothCategory of(String value) {
        return new BoothCategory(value);
    }
}
