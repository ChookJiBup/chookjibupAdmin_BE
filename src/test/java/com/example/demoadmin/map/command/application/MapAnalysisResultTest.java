package com.example.demoadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.application.port.DetectedMapObject;
import com.example.demoadmin.map.command.application.port.MapAnalysisResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MapAnalysisResultTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("분석 객체 목록이 null이면 생성할 수 없다")
        void fail_Constructor_CustomException_ObjectsNull() {
            // given
            List<DetectedMapObject> objects = null;

            // when & then
            assertThatThrownBy(() -> new MapAnalysisResult(objects))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
