package com.example.chookjibupadmin.map.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이미지 파일의 SHA-256 체크섬을 표현한다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sha256Checksum {

    private String value;

    private Sha256Checksum(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.value = value;
    }

    public static Sha256Checksum of(String value) {
        return new Sha256Checksum(value);
    }
}
