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
public class BoothLocation {

    private static final int MAX_LENGTH = 255;

    private String value;

    private BoothLocation(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        this.value = value;
    }

    public static BoothLocation of(String value) {
        return new BoothLocation(value);
    }
}
