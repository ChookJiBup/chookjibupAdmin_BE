package com.example.chookjibupadmin.map.command.infrastructure.image;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.example.chookjibupadmin.global.response.CustomException;
import com.example.chookjibupadmin.global.response.ErrorCode;
import com.example.chookjibupadmin.map.command.application.dto.MapImageUploadCommand;
import com.example.chookjibupadmin.map.command.application.dto.PreparedMapImage;
import com.example.chookjibupadmin.map.command.application.port.MapImagePreparationPort;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 업로드한 축제 도면을 검증하고 화면 표시용·AI 분석용 이미지를 생성한다.
 */
@Component
@RequiredArgsConstructor
public class MapImagePreparationService implements MapImagePreparationPort {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final MapImageProperties properties;

    @Override
    public PreparedMapImage prepare(MapImageUploadCommand command) {
        validateUpload(command);
        Path originalPath = null;
        Path displayPath = null;
        Path analysisPath = null;
        try {
            originalPath = Files.createTempFile(
                    "festival-blueprint-original-",
                    ".upload"
            );
            copyOriginal(command, originalPath);
            ImageFormat format = detectFormat(originalPath);
            BufferedImage originalImage = readValidatedImage(originalPath);
            int orientation = readOrientation(originalPath, format);
            BufferedImage displayImage = applyOrientation(
                    originalImage,
                    orientation
            );
            validateDimensions(displayImage.getWidth(), displayImage.getHeight());
            BufferedImage analysisImage = createAnalysisImage(displayImage);

            displayPath = Files.createTempFile(
                    "festival-blueprint-display-",
                    "." + format.extension
            );
            writeDisplayImage(displayImage, displayPath, format);
            analysisPath = Files.createTempFile(
                    "festival-blueprint-analysis-",
                    ".jpg"
            );
            writeAnalysisImage(analysisImage, analysisPath);

            return new PreparedMapImage(
                    sanitizeFileName(command.originalFileName(), format.extension),
                    originalPath,
                    displayPath,
                    analysisPath,
                    format.contentType,
                    format.contentType,
                    ImageFormat.JPEG.contentType,
                    format.extension,
                    format.extension,
                    ImageFormat.JPEG.extension,
                    Files.size(originalPath),
                    Files.size(displayPath),
                    Files.size(analysisPath),
                    displayImage.getWidth(),
                    displayImage.getHeight(),
                    analysisImage.getWidth(),
                    analysisImage.getHeight(),
                    checksum(originalPath),
                    checksum(displayPath),
                    checksum(analysisPath)
            );
        } catch (CustomException exception) {
            deleteQuietly(originalPath);
            deleteQuietly(displayPath);
            deleteQuietly(analysisPath);
            throw exception;
        } catch (Exception exception) {
            deleteQuietly(originalPath);
            deleteQuietly(displayPath);
            deleteQuietly(analysisPath);
            throw new CustomException(
                    ErrorCode.FESTIVAL_MAP_IMAGE_INVALID,
                    exception
            );
        }
    }

    private void validateUpload(MapImageUploadCommand command) {
        if (command == null || command.inputStreamSupplier() == null
                || command.fileSize() <= 0) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_FILE_EMPTY);
        }
        if (properties.maxFileSize() == null
                || command.fileSize() > properties.maxFileSize().toBytes()) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_FILE_TOO_LARGE);
        }
    }

    private void copyOriginal(MapImageUploadCommand command, Path originalPath)
            throws IOException {
        long maxFileSize = properties.maxFileSize().toBytes();
        long actualSize = 0;
        try (InputStream inputStream = command.inputStreamSupplier().open();
             OutputStream outputStream = Files.newOutputStream(originalPath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                actualSize += read;
                if (actualSize > maxFileSize) {
                    throw new CustomException(ErrorCode.FESTIVAL_MAP_FILE_TOO_LARGE);
                }
                outputStream.write(buffer, 0, read);
            }
        }
        if (actualSize == 0) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_FILE_EMPTY);
        }
        if (actualSize != command.fileSize()) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_IMAGE_INVALID);
        }
    }

    private ImageFormat detectFormat(Path path) throws IOException {
        byte[] header = new byte[8];
        try (InputStream inputStream = Files.newInputStream(path)) {
            if (inputStream.read(header) < header.length) {
                throw new CustomException(ErrorCode.FESTIVAL_MAP_IMAGE_INVALID);
            }
        }
        if (isJpeg(header)) {
            return ImageFormat.JPEG;
        }
        if (java.util.Arrays.equals(header, PNG_SIGNATURE)) {
            return ImageFormat.PNG;
        }
        throw new CustomException(ErrorCode.FESTIVAL_MAP_FILE_TYPE_NOT_ALLOWED);
    }

    private boolean isJpeg(byte[] header) {
        return header[0] == (byte) 0xFF
                && header[1] == (byte) 0xD8
                && header[2] == (byte) 0xFF;
    }

    private BufferedImage readValidatedImage(Path path) throws IOException {
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(path.toFile())) {
            if (imageInput == null) {
                throw new CustomException(ErrorCode.FESTIVAL_MAP_IMAGE_INVALID);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new CustomException(ErrorCode.FESTIVAL_MAP_IMAGE_INVALID);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, false, true);
                validateDecodedImageBounds(reader.getWidth(0), reader.getHeight(0));
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new CustomException(ErrorCode.FESTIVAL_MAP_IMAGE_INVALID);
                }
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    private void validateDimensions(int width, int height) {
        validateDecodedImageBounds(width, height);
        if (width < properties.minWidth() || height < properties.minHeight()) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_IMAGE_DIMENSION_INVALID);
        }
    }

    private void validateDecodedImageBounds(int width, int height) {
        long pixels = (long) width * height;
        if (width <= 0 || height <= 0
                || width > properties.maxOriginalSide()
                || height > properties.maxOriginalSide()
                || pixels > properties.maxOriginalPixels()) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_IMAGE_DIMENSION_INVALID);
        }
    }

    private BufferedImage createAnalysisImage(BufferedImage displayImage) {
        int maxSide = properties.analysisMaxSide();
        if (maxSide <= 0) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_IMAGE_INVALID);
        }
        int width = displayImage.getWidth();
        int height = displayImage.getHeight();
        int longestSide = Math.max(width, height);
        if (longestSide <= maxSide) {
            return displayImage;
        }
        double scale = (double) maxSide / longestSide;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage resized = new BufferedImage(
                targetWidth,
                targetHeight,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.setRenderingHint(
                    java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            graphics.setRenderingHint(
                    java.awt.RenderingHints.KEY_RENDERING,
                    java.awt.RenderingHints.VALUE_RENDER_QUALITY
            );
            graphics.drawImage(
                    displayImage,
                    0,
                    0,
                    targetWidth,
                    targetHeight,
                    null
            );
        } finally {
            graphics.dispose();
        }
        return resized;
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
        int targetWidth = swapDimensions ? height : width;
        int targetHeight = swapDimensions ? width : height;
        AffineTransform transform = orientationTransform(
                orientation,
                width,
                height
        );
        BufferedImage target = new BufferedImage(
                targetWidth,
                targetHeight,
                source.getColorModel().hasAlpha()
                        ? BufferedImage.TYPE_INT_ARGB
                        : BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(
                    java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );
            graphics.drawImage(source, transform, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private AffineTransform orientationTransform(
            int orientation,
            int width,
            int height
    ) {
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

    private void writeDisplayImage(
            BufferedImage image,
            Path path,
            ImageFormat format
    ) throws IOException {
        BufferedImage output = image;
        if (format == ImageFormat.JPEG && image.getTransparency() != Transparency.OPAQUE) {
            output = new BufferedImage(
                    image.getWidth(),
                    image.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D graphics = output.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, output.getWidth(), output.getHeight());
                graphics.drawImage(image, 0, 0, null);
            } finally {
                graphics.dispose();
            }
        }
        if (format == ImageFormat.JPEG) {
            writeJpeg(output, path, 0.92F);
            return;
        }
        if (!ImageIO.write(output, format.imageIoFormat, path.toFile())) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_IMAGE_INVALID);
        }
    }

    private void writeAnalysisImage(BufferedImage image, Path path)
            throws IOException {
        double configuredQuality = properties.analysisJpegQuality();
        if (configuredQuality <= 0 || configuredQuality > 1) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_IMAGE_INVALID);
        }
        writeJpeg(
                toOpaqueRgb(image),
                path,
                (float) configuredQuality
        );
    }

    private BufferedImage toOpaqueRgb(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB
                && image.getTransparency() == Transparency.OPAQUE) {
            return image;
        }
        BufferedImage output = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, output.getWidth(), output.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return output;
    }

    private void writeJpeg(
            BufferedImage image,
            Path path,
            float quality
    ) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new CustomException(ErrorCode.FESTIVAL_MAP_IMAGE_INVALID);
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(path.toFile())) {
            if (output == null) {
                throw new CustomException(ErrorCode.FESTIVAL_MAP_IMAGE_INVALID);
            }
            ImageWriteParam parameter = writer.getDefaultWriteParam();
            if (parameter.canWriteCompressed()) {
                parameter.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameter.setCompressionQuality(quality);
            }
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), parameter);
        } finally {
            writer.dispose();
        }
    }

    private String checksum(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String sanitizeFileName(String fileName, String extension) {
        String normalized = fileName == null ? "" : fileName
                .replaceAll("[\\p{Cntrl}]", "")
                .replace('\\', '/')
                .trim();
        int separator = normalized.lastIndexOf('/');
        if (separator >= 0) {
            normalized = normalized.substring(separator + 1);
        }
        if (normalized.isBlank()) {
            normalized = "festival-blueprint." + extension;
        }
        return normalized.length() <= 255
                ? normalized
                : normalized.substring(normalized.length() - 255);
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
        JPEG("image/jpeg", "jpg", "jpeg"),
        PNG("image/png", "png", "png");

        private final String contentType;
        private final String extension;
        private final String imageIoFormat;

        ImageFormat(String contentType, String extension, String imageIoFormat) {
            this.contentType = contentType;
            this.extension = extension;
            this.imageIoFormat = imageIoFormat;
        }
    }
}
