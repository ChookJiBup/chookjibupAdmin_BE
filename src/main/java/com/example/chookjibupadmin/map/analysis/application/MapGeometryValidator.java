package com.example.chookjibupadmin.map.analysis.application;

import com.example.chookjibupadmin.map.analysis.application.dto.AnalyzedMapNode;
import com.example.chookjibupadmin.map.roadmap.domain.GeometryType;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import org.springframework.stereotype.Component;

@Component
public class MapGeometryValidator {
    public boolean isValid(AnalyzedMapNode node) {
        if (node == null || node.nodeType() == null || node.geometryType() == null
                || node.name() == null || node.name().isBlank() || node.name().length() > 150
                || node.recognizedText() != null && node.recognizedText().length() > 500
                || node.geometry() == null || !node.geometry().isObject()
                || node.confidence() == null || node.confidence().signum() < 0
                || node.confidence().compareTo(java.math.BigDecimal.ONE) > 0) return false;
        return switch (node.geometryType()) {
            case POINT -> point(node.geometry());
            case RECTANGLE -> rectangle(node.geometry());
            case POLYGON -> points(node.geometry(), 3);
            case POLYLINE -> points(node.geometry(), 2);
        };
    }
    private boolean point(JsonNode value) { return normalized(value.get("x")) && normalized(value.get("y")); }
    private boolean rectangle(JsonNode value) {
        if (!point(value) || !positiveNormalized(value.get("width"))
                || !positiveNormalized(value.get("height")) || !finite(value.get("rotation"))) return false;
        return value.get("x").doubleValue()+value.get("width").doubleValue() <= 1.000001
                && value.get("y").doubleValue()+value.get("height").doubleValue() <= 1.000001;
    }
    private boolean points(JsonNode value, int minimum) {
        JsonNode points=value.get("points");
        if (points==null || !points.isArray() || points.size()<minimum) return false;
        Iterator<JsonNode> iterator=points.elements();
        while(iterator.hasNext()) if(!point(iterator.next())) return false;
        return true;
    }
    private boolean normalized(JsonNode n){return finite(n)&&n.doubleValue()>=0&&n.doubleValue()<=1;}
    private boolean positiveNormalized(JsonNode n){return finite(n)&&n.doubleValue()>0&&n.doubleValue()<=1;}
    private boolean finite(JsonNode n){return n!=null&&n.isNumber()&&Double.isFinite(n.doubleValue());}
}
