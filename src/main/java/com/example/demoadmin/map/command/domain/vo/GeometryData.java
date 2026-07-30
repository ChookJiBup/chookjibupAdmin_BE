package com.example.demoadmin.map.command.domain.vo;

import com.example.demoadmin.global.response.CustomException;
import com.example.demoadmin.global.response.ErrorCode;
import com.example.demoadmin.map.command.domain.GeometryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GeometryData {

    private static final int MAX_LENGTH = 4000;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String value;

    private GeometryData(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        validateGeometry(value);

        this.value = value;
    }

    private GeometryData(
            GeometryType expectedType,
            String value
    ) {
        if (expectedType == null || value == null || value.isBlank()
                || value.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        GeometryType actualType = geometryType(value);
        if (actualType != expectedType) {
            throw invalidRequest();
        }
        validateGeometry(value);
        this.value = value;
    }

    public static GeometryData of(String value) {
        return new GeometryData(value);
    }

    public static GeometryData of(
            GeometryType expectedType,
            String value
    ) {
        return new GeometryData(expectedType, value);
    }

    private static void validateGeometry(String value) {
        try {
            JsonNode geometry = OBJECT_MAPPER.readTree(value);
            if (!geometry.isObject()) {
                throw invalidRequest();
            }

            GeometryType type = geometryType(geometry);
            switch (type) {
                case POINT -> validatePoint(geometry);
                case RECTANGLE -> validateRectangle(geometry);
                case LINE -> validatePoints(geometry, 2);
                case POLYGON -> validatePoints(geometry, 3);
            }
        } catch (CustomException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidRequest();
        }
    }

    private static GeometryType geometryType(String value) {
        try {
            return geometryType(OBJECT_MAPPER.readTree(value));
        } catch (Exception exception) {
            throw invalidRequest();
        }
    }

    private static GeometryType geometryType(JsonNode geometry) {
        try {
            return GeometryType.valueOf(geometry.path("type").asText(""));
        } catch (Exception exception) {
            throw invalidRequest();
        }
    }

    private static void validatePoint(JsonNode geometry) {
        validateCoordinate(geometry, "x");
        validateCoordinate(geometry, "y");
    }

    private static void validateRectangle(JsonNode geometry) {
        double x = coordinate(geometry, "x");
        double y = coordinate(geometry, "y");
        double width = positiveCoordinate(geometry, "width");
        double height = positiveCoordinate(geometry, "height");
        if (x + width > 1 || y + height > 1) {
            throw invalidRequest();
        }
    }

    private static void validatePoints(
            JsonNode geometry,
            int minimumSize
    ) {
        JsonNode points = geometry.path("points");
        if (!points.isArray() || points.size() < minimumSize) {
            throw invalidRequest();
        }

        for (JsonNode point : points) {
            validatePoint(point);
        }
    }

    private static void validateCoordinate(
            JsonNode geometry,
            String field
    ) {
        coordinate(geometry, field);
    }

    private static double coordinate(
            JsonNode geometry,
            String field
    ) {
        JsonNode coordinate = geometry.get(field);
        if (coordinate == null || !coordinate.isNumber()) {
            throw invalidRequest();
        }

        double value = coordinate.asDouble();
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw invalidRequest();
        }
        return value;
    }

    private static double positiveCoordinate(
            JsonNode geometry,
            String field
    ) {
        double value = coordinate(geometry, field);
        if (value <= 0) {
            throw invalidRequest();
        }
        return value;
    }

    private static CustomException invalidRequest() {
        return new CustomException(ErrorCode.INVALID_REQUEST);
    }
}
