package net.diveon.backend.domain.problem.service;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import net.diveon.backend.domain.problem.others.ForDtoTestCase;
import net.diveon.backend.global.exception.InvalidSubmissionRequestException;

/** Baekjoon-style text input/output for rule models (text-v1, 2026-11-08). */
class OboTextContractTest {
 final ObjectMapper mapper=new ObjectMapper();
 static final String INPUT="7 3 FIFO\n7 0 1 2 0 3 0";
 static final String OUTPUT="7 Miss 7 - -\n0 Miss 7 0 -\n1 Miss 7 0 1\n2 Miss 2 0 1\n0 Hit 2 0 1\n3 Miss 2 3 1\n0 Miss 2 3 0\n6";
 JsonNode document() throws Exception {return mapper.readTree("""
 {"schemaVersion":2,"mode":"simulation","simulation":{"schemaVersion":2,"instances":[{"id":"p2","scenarioId":"P2","modelVersion":1,"name":"private name","input":{"refs":[9,8,9],"frameCount":2,"policy":"LRU","initial":[]},"views":[],"decorations":[],"labels":{}}],"question":{"selectionMode":"single","judgeProfile":"obo-v1","assessmentMode":"result","gradingInstanceId":"p2","ioFormat":"text-v1"},"explanations":{"common":["p2"],"choices":{}}}}
 """);}
 List<ForDtoTestCase> cases(String input,String output) throws Exception {
   var c=mapper.createObjectNode().put("index",1).put("input",input).put("isSample",true);
   if(output!=null)c.put("output",output);
   return List.of(mapper.treeToValue(c,ForDtoTestCase.class));
 }
 OboDocumentValidator validator(){return new OboDocumentValidator(mapper,"node","obo-runtime/checker.mjs");}
 @Test void exposesOnlyTheFormatNotModelValues() throws Exception {
   var response=OboPublicContract.describe(document());
   assertEquals("text-v1",response.path("ioFormat").asText());
   assertEquals("P2",response.path("scenarioId").asText());
   assertFalse(response.toString().contains("private name"));assertFalse(response.toString().contains("LRU"));
   assertFalse(response.has("inputSchema"));assertFalse(response.has("outputSchema"));
 }
 @Test void acceptsEngineCheckedTextCases() throws Exception {
   var v=validator();var doc=document();
   v.validateQuestion(doc,"coding",null);
   v.validateCodingCases(doc,cases(INPUT,OUTPUT));
   v.validateCodingCases(doc,cases(INPUT,"  "+OUTPUT.replace(" ","   ")+"\n\n"));
   v.validateCodingCases(doc,cases(INPUT,""));
 }
 @Test void rejectsWrongExpectedOutputAndUnreadableInput() throws Exception {
   var v=validator();var doc=document();
   assertThrows(InvalidSubmissionRequestException.class,()->v.validateCodingCases(doc,cases(INPUT,OUTPUT.replace("0 Hit","0 Miss"))));
   var error=assertThrows(InvalidSubmissionRequestException.class,()->v.validateCodingCases(doc,cases("7 3 FIFO\n7 0",null)));
   assertTrue(error.getMessage().contains("2번째 줄"),error.getMessage());
   assertThrows(InvalidSubmissionRequestException.class,()->v.validateCodingCases(doc,cases("{\"refs\":[1]}",null)));
 }
 @Test void refusesProcessModeForTextAnswers() throws Exception {
   var doc=document();((com.fasterxml.jackson.databind.node.ObjectNode)doc.at("/simulation/question")).put("assessmentMode","process");
   assertThrows(InvalidSubmissionRequestException.class,()->validator().validate(doc));
 }
}
