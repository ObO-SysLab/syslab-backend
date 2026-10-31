package net.diveon.backend.domain.problem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.diveon.backend.domain.problem.entity.*;

/** Content token for author edits; no DB schema or submission revision changes. */
public final class ProblemEditVersion {
    private ProblemEditVersion() {}
    public static String token(Problem p, ProblemObjective objective, ProblemCoding coding) {
        var mapper=new ObjectMapper();
        var value=mapper.createObjectNode().put("id",p.getId()).put("title",p.getTitle())
            .put("category",p.getCategory()).put("difficulty",p.getDifficulty()).put("visibility",p.getVisibility());
        if(objective!=null){
            value.put("summary",objective.getSummary()).put("description",objective.getDescription());
            value.set("choices",mapper.valueToTree(objective.getChoices()));value.set("answer",mapper.valueToTree(objective.getAnswer()));
            value.set("ordered",mapper.valueToTree(objective.getIsSeqeuntialAnswer()));value.set("obo",objective.getOboJsonData());
        }
        if(coding!=null){
            value.put("summary",coding.getSummary()).put("description",coding.getDescription())
                .put("inputDescription",coding.getInputDescription()).put("outputDescription",coding.getOutputDescription())
                .put("timeLimitMs",coding.getTimeLimitMs()).put("memoryLimitMb",coding.getMemoryLimitMb());
            value.set("languages",mapper.valueToTree(coding.getAllowedLanguages()));value.set("testcases",mapper.valueToTree(coding.getTestcases()));value.set("obo",coding.getOboJsonData());
        }
        return ProblemRevisionService.digest(ProblemRevisionService.canonical(value).toString());
    }
    public static void requireMatch(String expected,String current){
        if(expected!=null&&!expected.equals(current))throw new EditConflict();
    }
    @org.springframework.web.bind.annotation.ResponseStatus(value=org.springframework.http.HttpStatus.CONFLICT,reason="다른 창에서 수정되었습니다. 내 초안을 보관한 후 최신 자료를 확인하세요.")
    public static final class EditConflict extends RuntimeException {}
}
