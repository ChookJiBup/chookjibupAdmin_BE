package com.example.chookjibupadmin.map.analysis.infrastructure.openai;

import com.example.chookjibupadmin.map.analysis.application.MapAnalysisException;
import com.example.chookjibupadmin.map.analysis.application.dto.*;
import com.example.chookjibupadmin.map.analysis.application.port.MapBlueprintAnalysisPort;
import com.example.chookjibupadmin.map.roadmap.domain.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(prefix="app.map.analysis", name="provider", havingValue="openai")
public class OpenAiMapBlueprintAnalysisAdapter implements MapBlueprintAnalysisPort {
    private static final String PROMPT="""
            축제 도면에서 편집 가능한 시설을 탐지하세요. 좌표는 이미지 좌상단을 (0,0),
            우하단을 (1,1)로 하는 정규화 값이어야 합니다. 실제 거리를 추정하지 마세요.
            부스·무대·건물·공터·주차장은 가능한 영역 도형, 화장실·안내소·출입구는 점,
            통로·대기열은 선으로 반환하세요. 확실하지 않은 시설도 confidence로 표현하세요.
            """;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final MapAnalysisProperties properties;

    public OpenAiMapBlueprintAnalysisAdapter(
            @Qualifier("openAiMapRestClient") RestClient restClient,
            ObjectMapper objectMapper,
            MapAnalysisProperties properties
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override public MapAnalysisResult analyze(byte[] image,String contentType,int width,int height){
        try {
            String dataUrl="data:"+contentType+";base64,"+Base64.getEncoder().encodeToString(image);
            String body=restClient.post().uri("/v1/responses").contentType(MediaType.APPLICATION_JSON)
                    .body(request(dataUrl)).retrieve().body(String.class);
            JsonNode response=objectMapper.readTree(body);
            return parse(response);
        } catch(RestClientResponseException e){
            int status=e.getStatusCode().value();
            throw new MapAnalysisException("OPENAI_HTTP_"+status,"OpenAI request failed",status==429||status>=500,e);
        } catch(MapAnalysisException e){throw e;
        } catch(Exception e){throw new MapAnalysisException("OPENAI_RESPONSE_INVALID","OpenAI response is invalid",false,e);}
    }

    private Map<String,Object> request(String dataUrl){
        return Map.of("model",properties.modelOrDefault(),"store",false,
                "input",List.of(Map.of("role","user","content",List.of(
                        Map.of("type","input_text","text",PROMPT),
                        Map.of("type","input_image","image_url",dataUrl,"detail","original")))),
                "text",Map.of("format",Map.of("type","json_schema","name","festival_map_nodes",
                        "strict",true,"schema",schema())));
    }

    private Map<String,Object> schema(){
        Map<String,Object> nullableNumber=Map.of("type",List.of("number","null"));
        Map<String,Object> point=Map.of("type","object","properties",Map.of(
                "x",Map.of("type","number","minimum",0,"maximum",1),
                "y",Map.of("type","number","minimum",0,"maximum",1)),
                "required",List.of("x","y"),"additionalProperties",false);
        Map<String,Object> geometry=Map.of("type","object","properties",Map.of(
                "x",nullableNumber,"y",nullableNumber,"width",nullableNumber,
                "height",nullableNumber,"rotation",nullableNumber,
                "points",Map.of("type","array","items",point)),
                "required",List.of("x","y","width","height","rotation","points"),
                "additionalProperties",false);
        Map<String,Object> node=Map.of("type","object","properties",Map.of(
                "nodeType",Map.of("type","string","enum",Arrays.stream(NodeType.values()).map(Enum::name).toList()),
                "name",Map.of("type","string"),
                "geometryType",Map.of("type","string","enum",Arrays.stream(GeometryType.values()).map(Enum::name).toList()),
                "geometry",geometry,"confidence",Map.of("type","number","minimum",0,"maximum",1),
                "recognizedText",Map.of("type",List.of("string","null"))),
                "required",List.of("nodeType","name","geometryType","geometry","confidence","recognizedText"),
                "additionalProperties",false);
        return Map.of("type","object","properties",Map.of("nodes",Map.of("type","array","items",node)),
                "required",List.of("nodes"),"additionalProperties",false);
    }

    private MapAnalysisResult parse(JsonNode response) throws Exception {
        if(response==null) throw new MapAnalysisException("OPENAI_EMPTY","OpenAI returned no response",true);
        if("incomplete".equals(response.path("status").asText()))
            throw new MapAnalysisException("OPENAI_INCOMPLETE","OpenAI response incomplete",true);
        for(JsonNode output:response.path("output")) for(JsonNode content:output.path("content")) {
            if("refusal".equals(content.path("type").asText()))
                throw new MapAnalysisException("OPENAI_REFUSAL","OpenAI refused map analysis",false);
            if("output_text".equals(content.path("type").asText())) {
                JsonNode root=objectMapper.readTree(content.path("text").asText());
                List<AnalyzedMapNode> nodes=new ArrayList<>();
                for(JsonNode n:root.path("nodes")) nodes.add(new AnalyzedMapNode(
                        NodeType.valueOf(n.path("nodeType").asText()), n.path("name").asText(),
                        GeometryType.valueOf(n.path("geometryType").asText()), normalizeGeometry(n),
                        new BigDecimal(n.path("confidence").asText()),
                        n.path("recognizedText").isNull()?null:n.path("recognizedText").asText()));
                return new MapAnalysisResult(nodes);
            }
        }
        throw new MapAnalysisException("OPENAI_OUTPUT_MISSING","OpenAI output text missing",true);
    }

    private JsonNode normalizeGeometry(JsonNode node){
        ObjectNode source=(ObjectNode)node.path("geometry"); ObjectNode result=objectMapper.createObjectNode();
        GeometryType type=GeometryType.valueOf(node.path("geometryType").asText());
        if(type==GeometryType.POINT||type==GeometryType.RECTANGLE){result.set("x",source.get("x"));result.set("y",source.get("y"));}
        if(type==GeometryType.RECTANGLE){result.set("width",source.get("width"));result.set("height",source.get("height"));result.set("rotation",source.get("rotation"));}
        if(type==GeometryType.POLYGON||type==GeometryType.POLYLINE)result.set("points",source.get("points"));
        return result;
    }
}
