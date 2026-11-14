package net.diveon.backend.domain.problem.service;

import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.io.IOException;

/** Public input/output contract only; never includes values, labels, layouts or reference answers. */
public final class OboPublicContract {
    private OboPublicContract() {}
    private static volatile JsonNode contracts;
    private static volatile JsonNode diagramCatalog;
    private static JsonNode diagramCatalog(){
        var loaded=diagramCatalog;if(loaded!=null)return loaded;
        synchronized(OboPublicContract.class){
            if(diagramCatalog==null)try{
                diagramCatalog=new ObjectMapper().readTree(Files.readString(Path.of("obo-runtime/diagram-contract.json")));
            }catch(IOException error){throw new IllegalStateException("OBO diagram contracts unavailable",error);}
            return diagramCatalog;
        }
    }
    private static JsonNode contracts(){
        var loaded=contracts;if(loaded!=null)return loaded;
        synchronized(OboPublicContract.class){
            if(contracts==null)try{
                String source=Files.readString(Path.of("obo-runtime/contracts.mjs"));
                String json=source.substring(source.indexOf("export default ")+15).trim();
                if(json.endsWith(";"))json=json.substring(0,json.length()-1);
                contracts=new ObjectMapper().readTree(json).path("scenarios");
            }catch(IOException error){throw new IllegalStateException("OBO model contracts unavailable",error);}
            return contracts;
        }
    }
    public static JsonNode describe(JsonNode document){
        if(document!=null&&"coding_diff".equals(document.path("mode").asText())){
            var response=new ObjectMapper().createObjectNode().put("kind","diagram").put("modelVersion",1);
            // Baekjoon-style plain output: publish the line shape only (no IDs, labels, values or expected output).
            var format=document.path("codingDiff").path("outputFormat");
            if(format.isObject()){
                for(var node:document.path("codingDiff").path("nodes")){
                    if(!node.path("id").asText().equals(format.path("target").asText()))continue;
                    String type=node.path("type").asText();var data=node.path("data");
                    var shape=response.putObject("outputFormat").put("kind","lines-v1").put("targetType",type).put("blank",format.path("blank").asText("-"));
                    if("slot-grid".equals(type))shape.put("cells",data.path("slotCount").isInt()?data.path("slotCount").asInt():data.path("slots").size());
                    if("table-grid".equals(type))shape.put("columns",data.path("headers").size()>0?data.path("headers").size():data.path("rows").path(0).size());
                    return response;
                }
            }
            var nodes=response.putArray("nodes");var edgeIds=response.putArray("edgeIds");
            for(var node:document.path("codingDiff").path("nodes")){
                String type=node.path("type").asText();
                var item=nodes.addObject().put("id",node.path("id").asText()).put("type",type);
                var fields=diagramCatalog().path(type).path("fields");
                item.set("fields",fields.isObject()?fields.deepCopy():new ObjectMapper().createObjectNode());
            }
            for(var edge:document.path("codingDiff").path("edges"))edgeIds.add(edge.path("id").asText());
            return response;
        }
        var simulation=OboDocumentValidator.simulation(document);
        if(simulation==null||!"obo-v1".equals(simulation.path("question").path("judgeProfile").asText()))return null;
        var question=simulation.path("question");
        for(var instance:simulation.path("instances"))if(instance.path("id").asText().equals(question.path("gradingInstanceId").asText())){
            String scenario=instance.path("scenarioId").asText(),mode=question.path("assessmentMode").asText();
            var spec=contracts().get(scenario);if(spec==null)throw new IllegalStateException("Unsupported OBO model");
            var response=new ObjectMapper().createObjectNode().put("kind","simulation").put("modelVersion",1).put("scenarioId",scenario).put("assessmentMode",mode);
            if("rr-segments-v1".equals(question.path("ioFormat").asText())){
                if(!"RR".equals(scenario)||!"result".equals(mode))throw new IllegalStateException("Invalid RR output contract");
                return response.put("ioFormat","rr-segments-v1");
            }
            if("text-v1".equals(question.path("ioFormat").asText())){
                // Line formats are described by the shared runtime's textHelp; no JSON schema is published.
                if(!"result".equals(mode))throw new IllegalStateException("Invalid text output contract");
                return response.put("ioFormat","text-v1");
            }
            response.set("inputSchema",spec.path("inputSchema").deepCopy());
            response.set("outputSchema",spec.path("process".equals(mode)?"processOutputSchema":"resultOutputSchema").deepCopy());
            return response;
        }
        throw new IllegalStateException("OBO grading model missing");
    }
}
