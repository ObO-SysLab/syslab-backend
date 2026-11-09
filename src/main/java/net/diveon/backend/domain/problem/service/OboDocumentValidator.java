package net.diveon.backend.domain.problem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.*;
import net.diveon.backend.global.exception.InvalidSubmissionRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Runs only the versioned, application-owned checker; no submitted source or user-chosen path. */
@Service
public class OboDocumentValidator {
    private final ObjectMapper mapper;
    private final String node;
    private final Path checker;
    private final Semaphore permits=new Semaphore(4);

    public OboDocumentValidator(ObjectMapper mapper,
            @Value("${obo.node-executable:node}") String node,
            @Value("${obo.checker-path:obo-runtime/checker.mjs}") String checker) {
        this.mapper=mapper;this.node=node;this.checker=Path.of(checker).toAbsolutePath();
    }

    public void validate(JsonNode document) {
        if(document==null||document.isNull())return;
        if(!document.isObject()||document.toString().getBytes(StandardCharsets.UTF_8).length>1048576)
            throw invalid("OBO 문서는 1MiB 이하의 객체여야 합니다.");
        if(!"simulation".equals(document.path("mode").asText())&&!document.has("schemaVersion")){
            var request=mapper.createObjectNode().put("operation","validateFreeDiagram");
            request.set("document",document);
            var result=invoke(request);
            if(!result.path("ok").asBoolean(false))throw invalid(result.path("message").asText("그림 자료를 확인하세요."));
            return;
        }
        if(document.path("schemaVersion").asInt()!=2||!"simulation".equals(document.path("mode").asText())
                ||document.size()!=3||!document.path("simulation").isObject())throw invalid("지원하지 않는 OBO 문서 형식입니다.");
        var request=mapper.createObjectNode().put("operation","validateDocument");
        request.set("document",document.get("simulation"));
        JsonNode result=invoke(request);
        if(!result.path("ok").asBoolean(false))throw invalid(result.path("message").asText("OBO 설정이 올바르지 않습니다."));
    }

    private JsonNode invoke(JsonNode request) {
        if(!permits.tryAcquire())throw new IllegalStateException("OBO validator is busy; retry later");
        Process process=null;
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            process=new ProcessBuilder(node,"--max-old-space-size=96",checker.toString())
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            final Process running=process;
            var io=executor.submit(()->{
                try(var stdin=running.getOutputStream()){stdin.write(mapper.writeValueAsBytes(request));}
                byte[] output=running.getInputStream().readNBytes(1048577);
                if(output.length>1048576)throw new IllegalStateException("Checker response exceeds budget");
                return mapper.readTree(output);
            });
            try {
                if(!process.waitFor(Duration.ofSeconds(5).toMillis(),TimeUnit.MILLISECONDS))
                    throw new IllegalStateException("OBO validator timed out");
                JsonNode response=io.get(1,TimeUnit.SECONDS);
                if(response==null||!response.isObject()||!response.has("ok"))throw new IllegalStateException("Invalid checker response");
                if(process.exitValue()!=0&&process.exitValue()!=2)throw new IllegalStateException("OBO validator process failed");
                return response;
            } finally {running.destroyForcibly();}
        }catch(InterruptedException error){Thread.currentThread().interrupt();throw new IllegalStateException("OBO validation interrupted",error);}
        catch(java.io.IOException|ExecutionException|TimeoutException error){throw new IllegalStateException("OBO validator unavailable",error);}
        finally{if(process!=null)process.destroyForcibly();permits.release();}
    }

    public static JsonNode simulation(JsonNode document) {
        return document!=null&&"simulation".equals(document.path("mode").asText())?document.path("simulation"):null;
    }

    public void validateQuestion(JsonNode document,String problemType,java.util.List<Integer> answers) {
        validate(document);
        JsonNode simulation=simulation(document);
        if(simulation==null)return;
        var question=simulation.path("question");
        if("objective".equals(problemType)) {
            if(!"text-v1".equals(question.path("judgeProfile").asText()))throw invalid("객관식은 코딩 채점기를 사용할 수 없습니다.");
            if(answers==null||answers.isEmpty()||answers.stream().anyMatch(java.util.Objects::isNull)||answers.size()!=Set.copyOf(answers).size())throw invalid("중복 없는 정답을 선택하세요.");
            if("single".equals(question.path("selectionMode").asText())&&answers.size()!=1)throw invalid("단일 선택은 정답이 한 개여야 합니다.");
        }
    }

    public void validateChoices(JsonNode document,java.util.List<net.diveon.backend.domain.problem.others.ForDtoChoice> choices) {
        if(choices==null||choices.size()<2)throw invalid("객관식 보기는 두 개 이상 필요합니다.");
        var ids=new java.util.HashSet<String>();var indexes=new java.util.HashSet<Integer>();
        for(var choice:choices){
            if(choice==null||choice.getIndex()==null||choice.getIndex()<1||!indexes.add(choice.getIndex())
                    ||!choice.getId().matches("[A-Za-z0-9_-]{1,100}")||!ids.add(choice.getId()))throw invalid("보기 식별자가 잘못되었거나 중복되었습니다.");
            choice.setId(choice.getId());
        }
        var simulation=simulation(document);
        if(document!=null&&"per_choice".equals(document.path("mode").asText())){
            var keys=new java.util.HashSet<String>();
            choices.forEach(choice->keys.add("choice_"+choice.getIndex()));
            document.path("perChoice").fieldNames().forEachRemaining(key->{
                if(!keys.contains(key))throw invalid("해설이 삭제된 보기를 참조합니다: "+key);
            });
        }
        if(simulation!=null)simulation.path("explanations").path("choices").fieldNames().forEachRemaining(id->{
            if(!ids.contains(id))throw invalid("해설이 삭제된 보기를 참조합니다.");
        });
    }

    public void validateCodingCases(JsonNode document,java.util.List<net.diveon.backend.domain.problem.others.ForDtoTestCase> cases) {
        var simulation=simulation(document);
        if(simulation==null||!"obo-v1".equals(simulation.path("question").path("judgeProfile").asText()))return;
        String selected=simulation.path("question").path("gradingInstanceId").asText();
        String scenarioId=null;
        for(var instance:simulation.path("instances"))if(selected.equals(instance.path("id").asText()))scenarioId=instance.path("scenarioId").asText();
        if(scenarioId==null||cases==null||cases.isEmpty())throw invalid("의미 채점 모델과 테스트케이스가 필요합니다.");
        for(var testcase:cases){
            if(testcase==null||testcase.getInput()==null)throw invalid("테스트케이스 입력이 필요합니다.");
            if("rr-segments-v1".equals(simulation.path("question").path("ioFormat").asText())){
                if(!"RR".equals(scenarioId)||testcase.getOutput()==null)throw invalid("RR 모델과 공개 출력 형식이 필요합니다.");
                var request=mapper.createObjectNode().put("operation","rrGrade").put("input",testcase.getInput()).put("stdout",testcase.getOutput());
                var result=invoke(request);
                if(!result.path("ok").asBoolean()||!result.path("correct").asBoolean())throw invalid("테스트케이스 "+testcase.getIndex()+": "+result.path("message").asText("RR 입력·기대 출력을 확인하세요."));
                continue;
            }
            if("text-v1".equals(simulation.path("question").path("ioFormat").asText())){
                // Baekjoon-style text (2026-11-08): the engine computes the answer; a stored sample output must match it.
                boolean hasOutput=testcase.getOutput()!=null&&!testcase.getOutput().isBlank();
                var request=mapper.createObjectNode().put("operation",hasOutput?"textGrade":"textReference").put("scenarioId",scenarioId).put("input",testcase.getInput());
                if(hasOutput)request.put("stdout",testcase.getOutput());
                var result=invoke(request);
                if(!result.path("ok").asBoolean())throw invalid("테스트케이스 "+testcase.getIndex()+": "+result.path("message").asText("텍스트 입력을 확인하세요."));
                if(hasOutput&&!result.path("correct").asBoolean())throw invalid("테스트케이스 "+testcase.getIndex()+": 기대 출력이 모델 실행 결과와 다릅니다. 예제 버튼으로 다시 만들어 주세요.");
                continue;
            }
            try{
                var input=mapper.reader().with(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(testcase.getInput());
                var request=mapper.createObjectNode().put("operation","reference").put("scenarioId",scenarioId);request.set("input",input);
                var result=invoke(request);
                if(!result.path("ok").asBoolean())throw invalid("테스트케이스 "+testcase.getIndex()+": "+result.path("message").asText());
            }catch(java.io.IOException error){throw invalid("테스트케이스 "+testcase.getIndex()+"의 입력은 중복 키 없는 모델 JSON이어야 합니다.");}
        }
    }

    private static InvalidSubmissionRequestException invalid(String message){return new InvalidSubmissionRequestException(message);}
}
