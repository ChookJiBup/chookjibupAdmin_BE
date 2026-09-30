package com.example.chookjibupadmin.festival.command.infrastructure.image;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.example.chookjibupadmin.festival.command.application.dto.FestivalThumbnailUploadCommand;
import com.example.chookjibupadmin.festival.command.application.dto.PreparedFestivalThumbnail;
import com.example.chookjibupadmin.festival.command.application.port.FestivalThumbnailPreparationPort;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 축제 대표 이미지를 지도·AI 분석과 무관한 JPEG 썸네일로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class FestivalThumbnailPreparationService
        implements FestivalThumbnailPreparationPort {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final FestivalThumbnailProperties properties;

    @Override
    public PreparedFestivalThumbnail prepare(
            FestivalThumbnailUploadCommand command
    ) {
        validateUpload(command);
        Path originalPath = null;
        Path thumbnailPath = null;
        try {
            originalPath = Files.createTempFile("festival-thumbnail-original-", ".upload");
            copyOriginal(command, originalPath);
            ImageFormat format = detectFormat(originalPath);
            BufferedImage original = readValidatedImage(originalPath);
            BufferedImage oriented = applyOrientation(
                    original,
                    readOrientation(originalPath, format)
            );
            BufferedImage thumbnail = resize(oriented);

            thumbnailPath = Files.createTempFile("festival-thumbnail-", ".jpg");
            writeJpeg(thumbnail, thumbnailPath);
            return new PreparedFestivalThumbnail(
                    thumbnailPath,
                    Files.size(thumbnailPath),
                    "image/jpeg",
                    checksum(thumbnailPath),
                    thumbnail.getWidth(),
                    thumbnail.getHeight()
            );
        } catch (CustomException exception) {
            deleteQuietly(thumbnailPath);
            throw exception;
        } catch (Exception exception) {
            deleteQuietly(thumbnailPath);
            throw new CustomException(ErrorCode.FESTIVAL_IMAGE_INVALID, exception);
        } finally {
            deleteQuietly(originalPath);
        }
    }

    private void validateUpload(FestivalThumbnailUploadCommand command) {
        if (command == null || command.inputStreamSupplier() == null
                || command.fileSize() <= 0) {
            throw new CustomException(ErrorCode.FESTIVAL_IMAGE_FILE_EMPTY);
        }
        if (properties.maxFileSize() == null
                || command.fileSize() > properties.maxFileSize().toBytes()) {
            throw new CustomException(ErrorCode.FESTIVAL_IMAGE_FILE_TOO_LARGE);
        }
    }

    private void copyOriginal(
            FestivalThumbnailUploadCommand command,
            Path originalPath
    ) throws IOException {
        long maxFileSize = properties.maxFileSize().toBytes();
        long actualSize = 0;
        try (InputStream input = command.inputStreamSupplier().open();
             var output = Files.newOutputStream(originalPath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                actualSize += read;
                if (actualSize > maxFileSize) {
                    throw new CustomException(ErrorCode.FESTIVAL_IMAGE_FILE_TOO_LARGE);
                }
                output.write(buffer, 0, read);
            }
        }
        if (actualSize == 0) {
            throw new CustomException(ErrorCode.FESTIVAL_IMAGE_FILE_EMPTY);
        }
        if (actualSize != command.fileSize()) {
            throw new CustomException(ErrorCode.FESTIVAL_IMAGE_INVALID);
        }
    }

    private ImageFormat detectFormat(Path path) throws IOException {
        byte[] header = new byte[8];
        try (InputStream input = Files.newInputStream(path)) {
            if (input.read(header) < header.length) {
                throw new CustomException(ErrorCode.FESTIVAL_IMAGE_INVALID);
            }
        }
        if (header[0] == (byte) 0xFF
                && header[1] == (byte) 0xD8
                && header[2] == (byte) 0xFF) {
            return ImageFormat.JPEG;
        }
        if (java.util.Arrays.equals(header, PNG_SIGNATURE)) {
            return ImageFormat.PNG;
        }
        throw new CustomException(ErrorCode.FESTIVAL_IMAGE_FILE_TYPE_NOT_ALLOWED);
    }

    private BufferedImage readValidatedImage(Path path) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) {
                throw new CustomException(ErrorCode.FESTIVAL_IMAGE_INVALID);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new CustomException(ErrorCode.FESTIVAL_IMAGE_INVALID);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, true);
                validateBounds(reader.getWidth(0), reader.getHeight(0));
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new CustomException(ErrorCode.FESTIVAL_IMAGE_INVALID);
                }
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    private void validateBounds(int width, int height) {
        long pixels = (long) width * height;
        if (width <= 0 || height <= 0
                || width > properties.maxOriginalSide()
                || height > properties.maxOriginalSide()
                || pixels > properties.maxOriginalPixels()) {
            throw new CustomException(ErrorCode.FESTIVAL_IMAGE_INVALID);
        }
    }

    private BufferedImage resize(BufferedImage source) {
        int maxSide = properties.maxSide();
        if (maxSide <= 0) {
            throw new CustomException(ErrorCode.FESTIVAL_IMAGE_INVALID);
        }
        int longestSide = Math.max(source.getWidth(), source.getHeight());
        if (longestSide <= maxSide) {
            return toOpaqueRgb(source);
        }
        double scale = (double) maxSide / longestSide;
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private BufferedImage toOpaqueRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage output = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, output.getWidth(), output.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return output;
    }

    private void writeJpeg(BufferedImage image, Path path) throws IOException {
        double quality = properties.jpegQuality();
        if (quality <= 0 || quality > 1) {
            throw new CustomException(ErrorCode.FESTIVAL_IMAGE_INVALID);
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new CustomException(ErrorCode.FESTIVAL_IMAGE_INVALID);
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(path.toFile())) {
            ImageWriteParam parameter = writer.getDefaultWriteParam();
            if (parameter.canWriteCompressed()) {
                parameter.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameter.setCompressionQuality((float) quality);
            }
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), parameter);
        } finally {
            writer.dispose();
        }
    }

    private int readOrientation(Path path, ImageFormat format) {
        if (format != ImageFormat.JPEG) {
            return 1;
        }
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(
                    ExifIFD0Directory.class
            );
            return directory == null || !directory.containsTag(
                    ExifIFD0Directory.TAG_ORIENTATION
            ) ? 1 : directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (Exception ignored) {
            return 1;
        }
    }

    BufferedImage applyOrientation(BufferedImage source, int orientation) {
        if (orientation <= 1 || orientation > 8) {
            return source;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        boolean swapDimensions = orientation >= 5 && orientation <= 8;
        BufferedImage target = new BufferedImage(
                swapDimensions ? height : width,
                swapDimensions ? width : height,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.drawImage(
                    source,
                    orientationTransform(orientation, width, height),
                    null
            );
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private AffineTransform orientationTransform(int orientation, int width, int height) {
        AffineTransform transform = new AffineTransform();
        switch (orientation) {
            case 2 -> {
                transform.translate(width, 0);
                transform.scale(-1, 1);
            }
            case 3 -> {
                transform.translate(width, height);
                transform.rotate(Math.PI);
            }
            case 4 -> {
                transform.translate(0, height);
                transform.scale(1, -1);
            }
            case 5 -> {
                transform.rotate(Math.PI / 2);
                transform.scale(1, -1);
            }
            case 6 -> {
                transform.translate(height, 0);
                transform.rotate(Math.PI / 2);
            }
            case 7 -> {
                transform.translate(height, width);
                transform.rotate(Math.PI / 2);
                transform.scale(-1, 1);
            }
            case 8 -> {
                transform.translate(0, width);
                transform.rotate(-Math.PI / 2);
            }
            default -> {
                return new AffineTransform();
            }
        }
        return transform;
    }

    private String checksum(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 준비 실패의 원래 예외를 유지한다.
        }
    }

    private enum ImageFormat {
        JPEG,
        PNG
    }
}
