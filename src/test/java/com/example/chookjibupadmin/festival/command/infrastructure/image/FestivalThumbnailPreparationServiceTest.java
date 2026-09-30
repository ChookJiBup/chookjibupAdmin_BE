package com.example.chookjibupadmin.festival.command.infrastructure.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chookjibupadmin.festival.command.application.dto.FestivalThumbnailUploadCommand;
import com.example.chookjibupadmin.festival.command.application.dto.PreparedFestivalThumbnail;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class FestivalThumbnailPreparationServiceTest {

    private final FestivalThumbnailPreparationService service =
            new FestivalThumbnailPreparationService(
                    new FestivalThumbnailProperties(
                            DataSize.ofMegabytes(20),
                            12000,
                            50_000_000,
                            1600,
                            0.9
                    )
            );

    @Test
    @DisplayName("최소 해상도 제한 없이 작은 이미지를 대표 썸네일로 준비한다")
    void success_Prepare_SmallImageWithoutMinimumDimension() throws Exception {
        byte[] bytes = image("png", 32, 24);

        try (PreparedFestivalThumbnail prepared = service.prepare(command(
                "small-thumbnail.png",
                "image/png",
                bytes
        ))) {
            BufferedImage thumbnail = ImageIO.read(prepared.path().toFile());

            assertThat(prepared.contentType()).isEqualTo("image/jpeg");
            assertThat(prepared.width()).isEqualTo(32);
            assertThat(prepared.height()).isEqualTo(24);
            assertThat(thumbnail.getWidth()).isEqualTo(32);
            assertThat(thumbnail.getHeight()).isEqualTo(24);
            assertThat(prepared.checksumSha256()).hasSize(64);
        }
    }

    @Test
    @DisplayName("큰 대표 이미지는 비율을 유지하며 썸네일 최대 변으로 축소한다")
    void success_Prepare_LargeImageAsThumbnail() throws Exception {
        byte[] bytes = image("jpg", 2000, 1000);

        try (PreparedFestivalThumbnail prepared = service.prepare(command(
                "large-thumbnail.jpg",
                "image/jpeg",
                bytes
        ))) {
            assertThat(prepared.width()).isEqualTo(1600);
            assertThat(prepared.height()).isEqualTo(800);
        }
    }

    @Test
    @DisplayName("이미지 확장자를 가장한 파일은 대표 썸네일로 등록할 수 없다")
    void fail_Prepare_SpoofedImage() {
        byte[] bytes = "not-an-image".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.prepare(command(
                "fake.png",
                "image/png",
                bytes
        )))
                .isInstanceOf(CustomException.class)
                .hasMessage(
                        ErrorCode.FESTIVAL_IMAGE_FILE_TYPE_NOT_ALLOWED.getMessage()
                );
    }

    private FestivalThumbnailUploadCommand command(
            String fileName,
            String contentType,
            byte[] bytes
    ) {
        return new FestivalThumbnailUploadCommand(
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
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
