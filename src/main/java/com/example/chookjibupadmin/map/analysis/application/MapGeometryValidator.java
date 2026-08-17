package com.example.chookjibupadmin.map.analysis.application;

import com.example.chookjibupadmin.map.analysis.application.dto.AnalyzedMapNode;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Iterator;
import org.springframework.stereotype.Component;

@Component
public class MapGeometryValidator {

    private static final String SCHEMA_IMAGE = "1.0";
    private static final String SCHEMA_WGS84 = "2.0";

    public boolean isValid(AnalyzedMapNode node) {
        if (node == null
                || node.nodeType() == null
                || node.geometryType() == null
                || node.name() == null
                || node.name().isBlank()
                || node.name().length() > 150
                || node.recognizedText() != null
                && node.recognizedText().length() > 500
                || node.geometry() == null
                || !node.geometry().isObject()
                || node.confidence() == null
                || node.confidence().signum() < 0
                || node.confidence().compareTo(BigDecimal.ONE) > 0) {
            return false;
        }

        return isValid(SCHEMA_IMAGE, node.geometryType(), node.geometry());
    }

    public boolean isValid(String schemaVersion, GeometryType geometryType, JsonNode geometry) {
        if (geometryType == null || geometry == null || !geometry.isObject()) {
            return false;
        }
        if (SCHEMA_WGS84.equals(schemaVersion)) {
            return isValidWgs84(geometryType, geometry);
        }
        return isValid(geometryType, geometry);
    }

    public boolean isValid(GeometryType geometryType, JsonNode geometry) {
        if (geometryType == null || geometry == null || !geometry.isObject()) {
            return false;
        }

        return switch (geometryType) {
            case POINT -> point(geometry);
            case RECTANGLE -> rectangle(geometry);
            case POLYGON -> points(geometry, 3);
            case POLYLINE -> points(geometry, 2);
        };
    }

    private boolean isValidWgs84(GeometryType geometryType, JsonNode geometry) {
        return switch (geometryType) {
            case POINT -> wgs84Point(geometry);
            case POLYGON -> wgs84Points(geometry, 3);
            case POLYLINE -> wgs84Points(geometry, 2);
            case RECTANGLE -> false;
        };
    }

    private boolean wgs84Point(JsonNode value) {
        return wgs84Latitude(value.get("lat")) && wgs84Longitude(value.get("lng"));
    }

    private boolean wgs84Points(JsonNode value, int minimum) {
        JsonNode points = value.get("points");
        if (points == null || !points.isArray() || points.size() < minimum) {
            return false;
        }
        Iterator<JsonNode> iterator = points.elements();
        JsonNode previous = null;
        while (iterator.hasNext()) {
            JsonNode point = iterator.next();
            if (!wgs84Point(point)) {
                return false;
            }
            if (previous != null && sameWgs84Point(previous, point)) {
                return false;
            }
            previous = point;
        }
        return true;
    }

    private boolean sameWgs84Point(JsonNode left, JsonNode right) {
        return left.get("lat").doubleValue() == right.get("lat").doubleValue()
                && left.get("lng").doubleValue() == right.get("lng").doubleValue();
    }

    private boolean wgs84Latitude(JsonNode value) {
        return finite(value) && value.doubleValue() >= -90 && value.doubleValue() <= 90;
    }

    private boolean wgs84Longitude(JsonNode value) {
        return finite(value) && value.doubleValue() >= -180 && value.doubleValue() <= 180;
    }

    private boolean point(JsonNode value) {
        return normalized(value.get("x"))
                && normalized(value.get("y"));
    }

    private boolean rectangle(JsonNode value) {
        if (!point(value)
                || !positiveNormalized(value.get("width"))
                || !positiveNormalized(value.get("height"))
                || !finite(value.get("rotation"))) {
            return false;
        }

        return value.get("x").doubleValue()
                + value.get("width").doubleValue() <= 1.000001
                && value.get("y").doubleValue()
                + value.get("height").doubleValue() <= 1.000001;
    }

    private boolean points(JsonNode value, int minimum) {
        JsonNode points = value.get("points");
        if (points == null
                || !points.isArray()
                || points.size() < minimum) {
            return false;
        }

        Iterator<JsonNode> iterator = points.elements();
        while (iterator.hasNext()) {
            if (!point(iterator.next())) {
                return false;
            }
        }

        return true;
    }

    private boolean normalized(JsonNode value) {
        return finite(value)
                && value.doubleValue() >= 0
                && value.doubleValue() <= 1;
    }

    private boolean positiveNormalized(JsonNode value) {
        return finite(value)
                && value.doubleValue() > 0
                && value.doubleValue() <= 1;
    }

    private boolean finite(JsonNode value) {
        return value != null
                && value.isNumber()
                && Double.isFinite(value.doubleValue());
    }
}
