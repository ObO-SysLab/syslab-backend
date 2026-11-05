package net.diveon.backend.domain.problem.service;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import net.diveon.backend.domain.problem.others.ForDtoTestCase;

class OboRrContractTest {
 final ObjectMapper mapper=new ObjectMapper();
 JsonNode document() throws Exception {return mapper.readTree("""
 {"schemaVersion":2,"mode":"simulation","simulation":{"schemaVersion":2,"instances":[{"id":"rr","scenarioId":"RR","modelVersion":1,"name":"private name","input":{"jobs":[{"id":"P1","arrival":0,"bursts":[3]},{"id":"P2","arrival":1,"bursts":[1]}],"policy":"RR","quantum":2},"views":[],"decorations":[],"labels":{}}],"question":{"selectionMode":"single","judgeProfile":"obo-v1","assessmentMode":"result","gradingInstanceId":"rr","ioFormat":"rr-segments-v1"},"explanations":{"common":["rr"],"choices":{}}}}
 """);}
 List<ForDtoTestCase> cases(String output) throws Exception {
   var c=mapper.createObjectNode().put("index",1).put("input","2 2\nP1 0 3\nP2 1 1").put("output",output).put("isSample",true);
   return List.of(mapper.treeToValue(c,ForDtoTestCase.class));
 }
 @Test void exposesFormatWithoutModelValues() throws Exception {
   var response=OboPublicContract.describe(document());
   assertEquals("rr-segments-v1",response.path("ioFormat").asText());
   assertFalse(response.toString().contains("private name"));assertFalse(response.toString().contains("P1"));
   assertFalse(response.has("outputSchema"));
 }
 @Test void acceptsTextCasesAndRejectsMisleadingExpectedOutput() throws Exception {
   var v=new OboDocumentValidator(mapper,"node","obo-runtime/checker.mjs");var doc=document();
   v.validateQuestion(doc,"coding",null);v.validateCodingCases(doc,cases("P1 0 2\nP2 2 3\nP1 3 4"));
   assertThrows(net.diveon.backend.global.exception.InvalidSubmissionRequestException.class,()->v.validateCodingCases(doc,cases("P1 0 4")));
 }
 @Test void refusesProcessModeForIntervalOnlyAnswers() throws Exception {
   var doc=document();((com.fasterxml.jackson.databind.node.ObjectNode)doc.at("/simulation/question")).put("assessmentMode","process");
   assertThrows(net.diveon.backend.global.exception.InvalidSubmissionRequestException.class,()->new OboDocumentValidator(mapper,"node","obo-runtime/checker.mjs").validate(doc));
 }
}
