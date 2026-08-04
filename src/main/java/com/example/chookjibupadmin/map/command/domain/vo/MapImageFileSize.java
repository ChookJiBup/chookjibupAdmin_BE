package com.example.chookjibupadmin.map.command.domain.vo;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 저장된 이미지 파일의 바이트 크기를 표현한다.
 */
@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapImageFileSize {

    private long value;

    private MapImageFileSize(long value) {
        if (value <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.value = value;
    }

    public static MapImageFileSize of(long value) {
        return new MapImageFileSize(value);
    }
}
