package com.example.demoadmin.map.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MapObjectNameTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("지도 객체명을 생성한다")
        void success_Of() {
            // given
            String value = "김밥 부스";

            // when
            MapObjectName name = MapObjectName.of(value);

            // then
            assertThat(name.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("지도 객체명이 없으면 기본명으로 생성한다")
        void success_Of_BlankBoundary() {
            // given
            String value = " ";

            // when
            MapObjectName name = MapObjectName.of(value);

            // then
            assertThat(name.getValue()).isEqualTo("미확인 객체");
        }

        @Test
        @DisplayName("지도 객체명은 100자까지 허용한다")
        void success_Of_MaxLengthBoundary() {
            // given
            String value = "가".repeat(100);

            // when
            MapObjectName name = MapObjectName.of(value);

            // then
            assertThat(name.getValue()).hasSize(100);
        }

        @Test
        @DisplayName("지도 객체명이 100자를 넘으면 생성할 수 없다")
        void fail_Of_CustomException_OverMaxLengthBoundary() {
            // given
            String value = "가".repeat(101);

            // when & then
            assertThatThrownBy(() -> MapObjectName.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
