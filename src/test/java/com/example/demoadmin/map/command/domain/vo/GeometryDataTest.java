package com.example.demoadmin.map.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.domain.GeometryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GeometryDataTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("geometry JSON 문자열을 생성한다")
        void success_Of() {
            // given
            String value = """
                    {"type":"RECTANGLE","x":0.1,"y":0.2,"width":0.3,"height":0.4}
                    """.trim();

            // when
            GeometryData data = GeometryData.of(value);

            // then
            assertThat(data.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("geometry JSON 문자열은 4000자까지 허용한다")
        void success_Of_MaxLengthBoundary() {
            // given
            String prefix = "{\"type\":\"POINT\",\"x\":0.1,\"y\":0.2,\"memo\":\"";
            String suffix = "\"}";
            String value = prefix + "a".repeat(4000 - prefix.length() - suffix.length())
                    + suffix;

            // when
            GeometryData data = GeometryData.of(value);

            // then
            assertThat(data.getValue()).hasSize(4000);
        }

        @Test
        @DisplayName("geometry JSON 문자열이 비어 있으면 생성할 수 없다")
        void fail_Of_CustomException_Blank() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> GeometryData.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("정규화 좌표가 1을 넘으면 생성할 수 없다")
        void fail_Of_CustomException_CoordinateOutOfRange() {
            // given
            String value = "{\"type\":\"POINT\",\"x\":1.01,\"y\":0.2}";

            // when & then
            assertThatThrownBy(() -> GeometryData.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("사각형이 이미지 경계를 넘으면 생성할 수 없다")
        void fail_Of_CustomException_RectangleOutOfBounds() {
            // given
            String value = """
                    {"type":"RECTANGLE","x":0.9,"y":0.2,"width":0.2,"height":0.4}
                    """.trim();

            // when & then
            assertThatThrownBy(() -> GeometryData.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("선은 두 개 이상의 좌표가 필요하다")
        void fail_Of_CustomException_LinePointBoundary() {
            // given
            String value = """
                    {"type":"LINE","points":[{"x":0.1,"y":0.2}]}
                    """.trim();

            // when & then
            assertThatThrownBy(() -> GeometryData.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }

        @Test
        @DisplayName("요청한 geometry 타입과 JSON 타입이 다르면 생성할 수 없다")
        void fail_Of_CustomException_GeometryTypeMismatch() {
            // given
            String value = "{\"type\":\"POINT\",\"x\":0.1,\"y\":0.2}";

            // when & then
            assertThatThrownBy(() -> GeometryData.of(
                    GeometryType.RECTANGLE,
                    value
            ))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
