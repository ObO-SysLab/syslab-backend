package net.diveon.backend.domain.problem.service;

import com.fasterxml.jackson.databind.JsonNode;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemCoding;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.problem.repository.ProblemCodingRepository;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProblemOboService {

    private final ProblemRepository problemRepository;
    private final ProblemObjectiveRepository problemObjectiveRepository;
    private final ProblemCodingRepository problemCodingRepository;
    private final ProblemAccessService access;
    private final OboDisclosureService disclosure;
    private final net.diveon.backend.domain.grade.repository.SolveSubmissionRepository submissions;
    private final net.diveon.backend.domain.contest.repository.ContestSubmissionRepository contestSubmissions;
    private final net.diveon.backend.domain.grade.repository.SolveResultRepository results;

    public ProblemOboService(
        ProblemRepository problemRepository,
        ProblemObjectiveRepository problemObjectiveRepository,
        ProblemCodingRepository problemCodingRepository,
        ProblemAccessService access,
        OboDisclosureService disclosure,
        net.diveon.backend.domain.grade.repository.SolveSubmissionRepository submissions,
        net.diveon.backend.domain.contest.repository.ContestSubmissionRepository contestSubmissions,
        net.diveon.backend.domain.grade.repository.SolveResultRepository results
    ) {
        this.problemRepository = problemRepository;
        this.problemObjectiveRepository = problemObjectiveRepository;
        this.problemCodingRepository = problemCodingRepository;
        this.access = access;
        this.disclosure = disclosure;
        this.submissions = submissions;
        this.contestSubmissions = contestSubmissions;
        this.results = results;
    }

    @Transactional(readOnly = true)
    public JsonNode getOboJson(Long userId, Long problemId) {
        Problem problem = access.requireRead(userId, problemId);
        disclosure.requireExplanation(userId, problemId);
        JsonNode document = readDocument(problem, problemId);
        // New documents are disclosed only through their submission's immutable revision.
        if (OboDocumentValidator.simulation(document) != null) throw new ProblemNotFoundException();
        return document;
    }

    public record SubmissionObo(String revisionId, boolean legacyRevision, JsonNode obo, JsonNode publicCases,
            String lockedReason,java.time.LocalDateTime availableAt,JsonNode choices) {
        public SubmissionObo(String revisionId,boolean legacyRevision,JsonNode obo,JsonNode publicCases,String lockedReason,java.time.LocalDateTime availableAt){
            this(revisionId,legacyRevision,obo,publicCases,lockedReason,availableAt,null);
        }
    }

    public record ContestObo(Long probId, Boolean isCorrect, String resultStatus, JsonNode selectedAnswers, SubmissionObo explanation) {}

    @Transactional(readOnly = true)
    public ContestObo getContestSubmissionObo(Long userId, Long submissionId) {
        var submission=contestSubmissions.findById(submissionId).orElseThrow(ProblemNotFoundException::new);
        if(!java.util.Objects.equals(userId,submission.getUser().getId())
                ||!"COMPLETED".equals(submission.getSubmissionStatus()))throw new ProblemNotFoundException();
        var problem=access.requireReadInContest(userId,submission.getContestProblem().getProblem().getId(),submission.getContest().getId());
        var status=disclosure.status(userId,problem.getId());
        SubmissionObo result;
        if(!status.allowed())result=new SubmissionObo(null,false,null,null,status.reason(),status.availableAt());
        else {
            try {disclosure.requireContestEnded(submission.getContest());}
            catch(ProblemNotFoundException locked){return new ContestObo(problem.getId(),submission.getIsCorrect(),null,null,
                new SubmissionObo(null,false,null,null,"contest",submission.getContest().getEndTime()));}
            if(submission.getSolveSubmission()!=null)result=getSubmissionObo(userId,submission.getSolveSubmission().getId());
            else {
                var revision=submission.getProblemRevision();
                var document=revision==null?readDocument(problem,problem.getId()):revision.getSnapshot().get("obo");
                result=new SubmissionObo(revision==null?null:revision.getId(),revision==null,projectExplanation(document),
                    com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode(),null,null,revision==null?null:revision.getSnapshot().get("choices"));
            }
        }
        var solve = submission.getSolveSubmission();
        var verdict = result.lockedReason() == null && solve != null
                && solve.getSubmissionState() == net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState.COMPLETED
                ? results.findBySubmissionId(solve.getId()).orElse(null) : null;
        String resultStatus = result.lockedReason() == null
                ? net.diveon.backend.domain.grade.service.SubmissionResultStatus.fromContest(submission, verdict) : null;
        return new ContestObo(problem.getId(),submission.getIsCorrect(),resultStatus,submission.getObjectiveAnswer(),result);
    }

    @Transactional(readOnly = true)
    public SubmissionObo getSubmissionObo(Long userId, Long submissionId) {
        var submission = submissions.findById(submissionId).orElseThrow(ProblemNotFoundException::new);
        if (!java.util.Objects.equals(userId, submission.getUser().getId())
                || submission.getSubmissionState() != net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState.COMPLETED)
            throw new ProblemNotFoundException();
        var problem = access.requireRead(userId, submission.getProblem().getId());
        var status=disclosure.status(userId,problem.getId());
        if(!status.allowed())return new SubmissionObo(null,false,null,null,status.reason(),status.availableAt());
        var contestLink=contestSubmissions.findBySolveSubmissionId(submissionId);
        if(contestLink.isPresent())try{disclosure.requireContestEnded(contestLink.get().getContest());}
            catch(ProblemNotFoundException locked){return new SubmissionObo(null,false,null,null,"contest",contestLink.get().getContest().getEndTime());}
        var revision = submission.getProblemRevision();
        JsonNode document = revision == null ? readDocument(problem, problem.getId()) : revision.getSnapshot().get("obo");
        var publicCases=com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
        if(revision!=null&&submission.getPublicOboRecords()!=null){
            var cases=revision.getSnapshot().path("testcases");
            for(var record:submission.getPublicOboRecords()){
                int index=record.path("caseIndex").asInt(0)-1;
                if(index>=0&&index<cases.size()&&cases.get(index).path("isSample").isBoolean()&&cases.get(index).path("isSample").booleanValue()){
                    var publicRecord=com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
                    // inputText/expected: text-v1 example (2026-11-08); firstWrong: position of a wrong public output line (2026-11-09).
                    for(String field:java.util.List.of("caseIndex","scenarioId","input","reference","reported","correct","traceValid","message","omitted","kind","stdout","inputText","expected","firstWrong"))
                        if(record.has(field))publicRecord.set(field,record.get(field).deepCopy());
                    publicCases.add(publicRecord);
                }
            }
        }
        return new SubmissionObo(revision == null ? null : revision.getId(), revision == null, projectExplanation(document),publicCases,null,null,
            revision==null?null:revision.getSnapshot().get("choices"));
    }

    static JsonNode projectExplanation(JsonNode document) {
        if (document == null || document.isNull()) return null;
        JsonNode copy = document.deepCopy();
        var simulation = OboDocumentValidator.simulation(copy);
        if (simulation == null) return copy;
        java.util.Set<String> included = new java.util.HashSet<>();
        simulation.path("explanations").path("common").forEach(id -> included.add(id.asText()));
        simulation.path("explanations").path("choices").forEach(ids -> ids.forEach(id -> included.add(id.asText())));
        var instances = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
        simulation.path("instances").forEach(instance -> { if (included.contains(instance.path("id").asText())) instances.add(instance); });
        ((com.fasterxml.jackson.databind.node.ObjectNode)simulation).set("instances", instances);
        var question = (com.fasterxml.jackson.databind.node.ObjectNode)simulation.path("question");
        question.put("judgeProfile", "text-v1"); question.putNull("gradingInstanceId");
        return copy;
    }

    @Transactional(readOnly = true)
    public JsonNode getAuthorOboJson(Long userId, Long problemId) {
        Problem problem = access.requireRead(userId, problemId);
        disclosure.requireAuthor(userId, problem);
        return readDocument(problem, problemId);
    }

    @Transactional
    public java.util.Map<String,Object> getAuthorSettings(Long userId,Long problemId) {
        var problem=access.requireRead(userId,problemId);disclosure.requireAuthor(userId,problem);
        problem=problemRepository.findForRevision(problemId).orElseThrow(ProblemNotFoundException::new);
        var result=new java.util.HashMap<String,Object>();result.put("oboJson",readDocument(problem,problemId));
        result.put("title",problem.getTitle());result.put("category",problem.getCategory());
        result.put("difficulty",problem.getDifficulty());result.put("visibility",problem.getVisibility());
        if("objective".equals(problem.getType())){
            var objective=problemObjectiveRepository.findById(problemId).orElseThrow(ProblemNotFoundException::new);
            result.put("editVersion",ProblemEditVersion.token(problem,objective,null));
            result.put("summary",objective.getSummary());result.put("description",objective.getDescription());
            result.put("answer",objective.getAnswer());result.put("choices",objective.getChoices());
            result.put("isSeqeuntialAnswer",objective.getIsSeqeuntialAnswer());
        }else if("coding".equals(problem.getType())){
            var coding=problemCodingRepository.findById(problemId).orElseThrow(ProblemNotFoundException::new);
            result.put("editVersion",ProblemEditVersion.token(problem,null,coding));
            result.put("summary",coding.getSummary());result.put("description",coding.getDescription());
            result.put("testcases",coding.getTestcases());result.put("inputDescription",coding.getInputDescription());result.put("outputDescription",coding.getOutputDescription());
            result.put("constraints",new net.diveon.backend.domain.problem.dto.response.ProblemDetailCodingResponse.Constraints(
                coding.getTimeLimitMs(),coding.getMemoryLimitMb(),coding.getAllowedLanguages()));
        }
        return result;
    }

    private JsonNode readDocument(Problem problem, Long problemId) {
        if ("objective".equals(problem.getType())) {
            ProblemObjective problemObjective = problemObjectiveRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException(problemId + "번 객관식 문제가 존재하지 않습니다."));
            return toResponseOrNull(problemObjective);
        }

        if ("coding".equals(problem.getType())) {
            ProblemCoding problemCoding = problemCodingRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException(problemId + "번 코딩 문제가 존재하지 않습니다."));
            return toResponseOrNull(problemCoding);
        }

        return null;
    }

    private JsonNode toResponseOrNull(ProblemObjective problemObjective) {
        return problemObjective.getOboJsonData();
    }

    private JsonNode toResponseOrNull(ProblemCoding problemCoding) {
        return problemCoding.getOboJsonData();
    }
}
