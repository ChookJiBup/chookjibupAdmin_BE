package com.example.demoadmin.map.command.domain.vo;

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
public class ConfidenceScore {

    private double value;

    private ConfidenceScore(double value) {
        if (Double.isNaN(value) || value < 0 || value > 1) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        this.value = value;
    }

    public static ConfidenceScore of(double value) {
        return new ConfidenceScore(value);
    }
}
