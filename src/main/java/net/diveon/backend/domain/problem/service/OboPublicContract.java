package net.diveon.backend.domain.problem.service;

import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.io.IOException;

/** Static model schemas only; never includes a problem's model inputs, answers or private cases. */
public final class OboPublicContract {
    private OboPublicContract() {}
    private static volatile JsonNode contracts;
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
        var simulation=OboDocumentValidator.simulation(document);
        if(simulation==null||!"obo-v1".equals(simulation.path("question").path("judgeProfile").asText()))return null;
        var question=simulation.path("question");
        for(var instance:simulation.path("instances"))if(instance.path("id").asText().equals(question.path("gradingInstanceId").asText())){
            String scenario=instance.path("scenarioId").asText(),mode=question.path("assessmentMode").asText();
            var spec=contracts().get(scenario);if(spec==null)throw new IllegalStateException("Unsupported OBO model");
            var response=new ObjectMapper().createObjectNode().put("modelVersion",1).put("scenarioId",scenario).put("assessmentMode",mode);
            response.set("inputSchema",spec.path("inputSchema").deepCopy());
            response.set("outputSchema",spec.path("process".equals(mode)?"processOutputSchema":"resultOutputSchema").deepCopy());
            return response;
        }
        throw new IllegalStateException("OBO grading model missing");
    }
}
