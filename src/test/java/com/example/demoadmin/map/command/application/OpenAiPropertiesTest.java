package com.example.demoadmin.map.command.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demoadmin.map.command.infrastructure.openai.OpenAiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenAiPropertiesTest {

    @Nested
    @DisplayName("resolvedBaseUrl")
    class ResolvedBaseUrl {

        @Test
        @DisplayName("baseUrl이 없으면 기본 OpenAI API URL을 사용한다")
        void success_ResolvedBaseUrl_BlankBoundary() {
            // given
            OpenAiProperties properties = new OpenAiProperties("key", " ", "gpt-5.6");

            // when
            String result = properties.resolvedBaseUrl();

            // then
            assertThat(result).isEqualTo("https://api.openai.com/v1");
        }
    }

    @Nested
    @DisplayName("resolvedModel")
    class ResolvedModel {

        @Test
        @DisplayName("model이 없으면 기본 모델을 사용한다")
        void success_ResolvedModel_BlankBoundary() {
            // given
            OpenAiProperties properties = new OpenAiProperties("key", "http://localhost", " ");

            // when
            String result = properties.resolvedModel();

            // then
            assertThat(result).isEqualTo("gpt-5.6");
        }
    }
}
