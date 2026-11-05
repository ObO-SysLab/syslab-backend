package net.diveon.backend.domain.problem.service;

import java.util.*;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.global.exception.InvalidSubmissionRequestException;

public final class ObjectiveAnswerRules {
    private ObjectiveAnswerRules() {}
    public static void validate(ProblemObjective problem,List<?> answers){
        if(answers==null||answers.isEmpty()||answers.stream().anyMatch(a->!(a instanceof Integer))
                ||new HashSet<>(answers).size()!=answers.size())throw invalid();
        if(problem.getChoices()==null)throw invalid();
        var known=new HashSet<Integer>();problem.getChoices().forEach(choice->known.add(choice.getIndex()));
        if(!known.containsAll(answers))throw invalid();
        var simulation=OboDocumentValidator.simulation(problem.getOboJsonData());
        if(simulation!=null&&"single".equals(simulation.path("question").path("selectionMode").asText())&&answers.size()!=1)throw invalid();
    }
    public static void validateSnapshot(com.fasterxml.jackson.databind.JsonNode snapshot,List<Integer> answers){
        if(answers==null||answers.isEmpty()||answers.stream().anyMatch(Objects::isNull)||new HashSet<>(answers).size()!=answers.size())throw invalid();
        var known=new HashSet<Integer>();snapshot.path("choices").forEach(choice->known.add(choice.path("index").intValue()));
        if(!known.containsAll(answers))throw invalid();
        if("single".equals(snapshot.path("question").path("selectionMode").asText())&&answers.size()!=1)throw invalid();
    }
    private static InvalidSubmissionRequestException invalid(){return new InvalidSubmissionRequestException("문제의 선택 형식에 맞는 중복 없는 보기 번호를 제출하세요.");}
}
