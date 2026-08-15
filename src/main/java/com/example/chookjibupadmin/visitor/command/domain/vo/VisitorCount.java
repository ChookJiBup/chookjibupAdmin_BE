package com.example.chookjibupadmin.visitor.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 축제 방문 인원 수를 표현하는 값 객체이다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitorCount {

    private int value;

    private VisitorCount(int value) {
        validate(value);
        this.value = value;
    }

    public static VisitorCount of(int value) {
        return new VisitorCount(value);
    }

    private void validate(int value) {
        if (value < 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
