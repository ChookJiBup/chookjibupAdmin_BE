package com.example.chookjibupadmin.map.command.infrastructure.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.MapImageUploadCommand;
import com.example.chookjibupadmin.map.command.application.dto.PreparedMapImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class MapImagePreparationServiceTest {

    private final MapImagePreparationService service =
            new MapImagePreparationService(new MapImageProperties(
                    DataSize.ofMegabytes(20),
                    800,
                    600,
                    12000,
                    50_000_000,
                    4096,
                    0.95
            ));

    @Test
    @DisplayName("정상 PNG를 검증하고 original, display, analysis 파일을 준비한다")
    void success_Prepare_Png() throws Exception {
        byte[] bytes = image("png", 800, 600);

        try (PreparedMapImage prepared = service.prepare(command(
                "festival-map.png",
                "image/png",
                bytes
        ))) {
            assertThat(prepared.originalContentType()).isEqualTo("image/png");
            assertThat(prepared.displayContentType()).isEqualTo("image/png");
            assertThat(prepared.analysisContentType()).isEqualTo("image/jpeg");
            assertThat(prepared.displayImageWidth()).isEqualTo(800);
            assertThat(prepared.displayImageHeight()).isEqualTo(600);
            assertThat(prepared.analysisImageWidth()).isEqualTo(800);
            assertThat(prepared.analysisImageHeight()).isEqualTo(600);
            assertThat(prepared.originalChecksumSha256()).hasSize(64);
            assertThat(prepared.displayChecksumSha256()).hasSize(64);
            assertThat(prepared.analysisChecksumSha256()).hasSize(64);
            assertThat(prepared.originalPath()).exists();
            assertThat(prepared.displayPath()).exists();
            assertThat(prepared.analysisPath()).exists();
        }
    }

    @Test
    @DisplayName("큰 원본 도면은 유지하고 AI 분석본만 최대 변 길이에 맞춰 축소한다")
    void success_Prepare_LargeBlueprintForAnalysis() throws Exception {
        byte[] bytes = image("png", 5000, 1000);

        try (PreparedMapImage prepared = service.prepare(command(
                "large-festival-blueprint.png",
                "image/png",
                bytes
        ))) {
            BufferedImage original = ImageIO.read(prepared.originalPath().toFile());
            BufferedImage display = ImageIO.read(prepared.displayPath().toFile());
            BufferedImage analysis = ImageIO.read(prepared.analysisPath().toFile());

            assertThat(original.getWidth()).isEqualTo(5000);
            assertThat(display.getWidth()).isEqualTo(5000);
            assertThat(analysis.getWidth()).isEqualTo(4096);
            assertThat(analysis.getHeight()).isEqualTo(819);
        }
    }

    @Test
    @DisplayName("JPEG display 이미지는 좌표 기준 크기를 유지한 채 다시 인코딩한다")
    void success_Prepare_Jpeg() throws Exception {
        byte[] bytes = image("jpg", 800, 600);

        try (PreparedMapImage prepared = service.prepare(command(
                "festival-map.jpg",
                "image/jpeg",
                bytes
        ))) {
            BufferedImage display = ImageIO.read(prepared.displayPath().toFile());

            assertThat(prepared.displayContentType()).isEqualTo("image/jpeg");
            assertThat(display.getWidth()).isEqualTo(800);
            assertThat(display.getHeight()).isEqualTo(600);
        }
    }

    @Test
    @DisplayName("EXIF Orientation 7은 축을 바꾸고 모든 픽셀을 캔버스 안에 배치한다")
    void success_ApplyOrientation_Seven() {
        BufferedImage source = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
        source.setRGB(0, 0, Color.RED.getRGB());
        source.setRGB(1, 0, Color.GREEN.getRGB());
        source.setRGB(0, 2, Color.BLUE.getRGB());
        source.setRGB(1, 2, Color.YELLOW.getRGB());

        BufferedImage oriented = service.applyOrientation(source, 7);

        assertThat(oriented.getWidth()).isEqualTo(3);
        assertThat(oriented.getHeight()).isEqualTo(2);
        assertThat(oriented.getRGB(2, 1)).isEqualTo(Color.RED.getRGB());
        assertThat(oriented.getRGB(2, 0)).isEqualTo(Color.GREEN.getRGB());
        assertThat(oriented.getRGB(0, 1)).isEqualTo(Color.BLUE.getRGB());
        assertThat(oriented.getRGB(0, 0)).isEqualTo(Color.YELLOW.getRGB());
    }

    @Test
    @DisplayName("확장자와 MIME이 PNG여도 실제 이미지가 아니면 거절한다")
    void fail_Prepare_SpoofedPng() {
        byte[] bytes = "not-an-image".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.prepare(command(
                "fake.png",
                "image/png",
                bytes
        )))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FESTIVAL_MAP_FILE_TYPE_NOT_ALLOWED.getMessage());
    }

    @Test
    @DisplayName("최소 해상도보다 작은 이미지는 거절한다")
    void fail_Prepare_SmallImage() throws Exception {
        byte[] bytes = image("png", 799, 600);

        assertThatThrownBy(() -> service.prepare(command(
                "small.png",
                "image/png",
                bytes
        )))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FESTIVAL_MAP_IMAGE_DIMENSION_INVALID.getMessage());
    }

    @Test
    @DisplayName("빈 파일은 거절한다")
    void fail_Prepare_Empty() {
        assertThatThrownBy(() -> service.prepare(command(
                "empty.png",
                "image/png",
                new byte[0]
        )))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FESTIVAL_MAP_FILE_EMPTY.getMessage());
    }

    @Test
    @DisplayName("선언한 크기와 실제 스트림 크기가 다르면 거절한다")
    void fail_Prepare_SizeMismatch() {
        MapImageUploadCommand mismatched = new MapImageUploadCommand(
                "map.png",
                "image/png",
                2,
                () -> new ByteArrayInputStream(new byte[]{1})
        );

        assertThatThrownBy(() -> service.prepare(mismatched))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FESTIVAL_MAP_IMAGE_INVALID.getMessage());
    }

    @Test
    @DisplayName("실제 스트림이 제한 용량을 넘으면 복사 중 즉시 중단한다")
    void fail_Prepare_ActualStreamTooLarge() {
        MapImagePreparationService smallLimitService =
                new MapImagePreparationService(new MapImageProperties(
                        DataSize.ofBytes(2),
                        1,
                        1,
                        10,
                        100,
                        10,
                        0.95
                ));
        MapImageUploadCommand oversized = new MapImageUploadCommand(
                "map.png",
                "image/png",
                2,
                () -> new ByteArrayInputStream(new byte[]{1, 2, 3})
        );

        assertThatThrownBy(() -> smallLimitService.prepare(oversized))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FESTIVAL_MAP_FILE_TOO_LARGE.getMessage());
    }

    private MapImageUploadCommand command(
            String fileName,
            String contentType,
            byte[] bytes
    ) {
        return new MapImageUploadCommand(
                fileName,
                contentType,
                bytes.length,
                () -> new ByteArrayInputStream(bytes)
        );
    }

    private byte[] image(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.BLUE);
            graphics.fillRect(50, 50, 200, 100);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
