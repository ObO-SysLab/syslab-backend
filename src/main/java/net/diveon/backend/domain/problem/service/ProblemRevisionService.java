package net.diveon.backend.domain.problem.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.TreeSet;
import net.diveon.backend.domain.problem.entity.*;
import net.diveon.backend.domain.problem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProblemRevisionService {
    private final ProblemRevisionRepository revisions;
    private final ProblemObjectiveRepository objectives;
    private final ProblemCodingRepository codings;
    private final ObjectMapper mapper;
    public ProblemRevisionService(ProblemRevisionRepository revisions,ProblemObjectiveRepository objectives,
            ProblemCodingRepository codings,ObjectMapper mapper){
        this.revisions=revisions;this.objectives=objectives;this.codings=codings;this.mapper=mapper;
    }
    /** Caller holds the problem row lock, or has just inserted the problem in this transaction. */
    @Transactional
    public ProblemRevision capture(Problem problem){
        if(!"objective".equals(problem.getType())&&!"coding".equals(problem.getType()))return null;
        ObjectNode snapshot=mapper.createObjectNode().put("v",1).put("type",problem.getType())
            .put("title",problem.getTitle()).put("modelVersion",1).put("judgeProfile","text-v1");
        JsonNode obo;
        if("objective".equals(problem.getType())){
            var objective=objectives.findById(problem.getId()).orElseThrow();
            snapshot.put("summary",objective.getSummary()).put("description",objective.getDescription());
            snapshot.set("choices",mapper.valueToTree(objective.getChoices()));
            snapshot.set("answer",mapper.valueToTree(objective.getAnswer()));
            snapshot.put("ordered",Boolean.TRUE.equals(objective.getIsSeqeuntialAnswer()));
            obo=objective.getOboJsonData();
        }else{
            var coding=codings.findById(problem.getId()).orElseThrow();
            snapshot.put("summary",coding.getSummary()).put("description",coding.getDescription())
                .put("inputDescription",coding.getInputDescription()).put("outputDescription",coding.getOutputDescription())
                .put("timeLimitMs",coding.getTimeLimitMs()).put("memoryLimitMb",coding.getMemoryLimitMb());
            snapshot.set("allowedLanguages",mapper.valueToTree(coding.getAllowedLanguages()));
            snapshot.set("testcases",mapper.valueToTree(coding.getTestcases()));
            obo=coding.getOboJsonData();
        }
        snapshot.set("obo",obo==null?NullNode.instance:obo.deepCopy());
        var simulation=OboDocumentValidator.simulation(obo);
        if(simulation!=null){
            snapshot.set("question",simulation.path("question").deepCopy());
            snapshot.put("judgeProfile",simulation.path("question").path("judgeProfile").asText("text-v1"));
        }
        JsonNode canonical=canonical(snapshot);
        String id=problem.getId()+":"+digest(canonical.toString());
        return revisions.findById(id).orElseGet(()->revisions.save(new ProblemRevision(id,problem.getId(),canonical)));
    }
    static JsonNode canonical(JsonNode value){
        if(value.isObject()){
            var result=JsonNodeFactory.instance.objectNode();var keys=new TreeSet<String>();
            value.fieldNames().forEachRemaining(keys::add);keys.forEach(key->result.set(key,canonical(value.get(key))));return result;
        }
        if(value.isArray()){var result=JsonNodeFactory.instance.arrayNode();value.forEach(item->result.add(canonical(item)));return result;}
        return value.deepCopy();
    }
    static String digest(String value){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
