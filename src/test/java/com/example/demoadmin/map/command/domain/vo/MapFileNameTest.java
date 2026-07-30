package com.example.demoadmin.map.command.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MapFileNameTest {

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("배치도 파일명을 생성한다")
        void success_Of() {
            // given
            String value = "김밥축제_지적편집도.png";

            // when
            MapFileName fileName = MapFileName.of(value);

            // then
            assertThat(fileName.getValue()).isEqualTo(value);
        }

        @Test
        @DisplayName("배치도 파일명이 비어 있으면 생성할 수 없다")
        void fail_Of_CustomException_Blank() {
            // given
            String value = " ";

            // when & then
            assertThatThrownBy(() -> MapFileName.of(value))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.INVALID_REQUEST.getMessage());
        }
    }
}
