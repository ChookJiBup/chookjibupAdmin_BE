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
public class BoothDescription {

    private static final int MAX_LENGTH = 1000;

    private String value;

    private BoothDescription(String value) {
        if (value == null) {
            this.value = "";
            return;
        }
        if (value.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        this.value = value;
    }

    public static BoothDescription of(String value) {
        return new BoothDescription(value);
    }
}
