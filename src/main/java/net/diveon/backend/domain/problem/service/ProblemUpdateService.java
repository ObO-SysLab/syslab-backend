package net.diveon.backend.domain.problem.service;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.problem.dto.request.ProblemUpdateObjectiveRequest;
import net.diveon.backend.domain.problem.dto.request.ProblemUpdatePracticeRequest;
import net.diveon.backend.domain.problem.dto.request.ProblemUpdateCodingRequest;
import net.diveon.backend.domain.problem.dto.response.ProblemCreateObjectiveResponse;
import net.diveon.backend.domain.problem.entity.OboStep;
import net.diveon.backend.domain.problem.dto.response.ProblemUpdateObjectiveResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemUpdatePracticeResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemUpdateCodingResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.problem.entity.ProblemPractice;
import net.diveon.backend.domain.problem.entity.ProblemCoding;
import net.diveon.backend.domain.problem.others.ForDtoOboStep;
import net.diveon.backend.domain.problem.repository.OboStepRepository;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.problem.repository.ProblemCodingRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.ProblemAccessDeniedException;
import net.diveon.backend.global.exception.ProblemNotFoundException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;


@Service
public class ProblemUpdateService {
    private final ProblemRepository problemRepository;
    private final ProblemObjectiveRepository problemObjectiveRepository;
    private final ProblemPracticeRepository problemPracticeRepository;
    private final ProblemCodingRepository problemCodingRepository;
    private final OboStepRepository oboStepRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final OboDocumentValidator oboValidator;
    private final ProblemRevisionService revisions;

    public ProblemUpdateService(ProblemRepository problemRepository,
        ProblemObjectiveRepository problemObjectiveRepository,
        ProblemPracticeRepository problemPracticeRepository,
        ProblemCodingRepository problemCodingRepository,
        OboStepRepository oboStepRepository,
        UserRepository userRepository,
        S3Service s3Service, OboDocumentValidator oboValidator, ProblemRevisionService revisions){
            this.problemObjectiveRepository = problemObjectiveRepository;
            this.problemPracticeRepository = problemPracticeRepository;
            this.problemCodingRepository = problemCodingRepository;
            this.problemRepository = problemRepository;
            this.oboStepRepository = oboStepRepository;
            this.userRepository = userRepository;
            this.s3Service = s3Service;
            this.oboValidator = oboValidator;
            this.revisions = revisions;
        }

    // 객관식형
    private void checkOwner(Problem problem, long userId) {
        if (!problem.getAuthor().getId().equals(userId)) {
            throw new ProblemAccessDeniedException();
        }
    }

    @Transactional
    public ProblemUpdateObjectiveResponse updateProblemObjective(long userId, long prodId,
        ProblemUpdateObjectiveRequest request){
            Problem problem = problemRepository.findForRevision(prodId).orElseThrow(ProblemNotFoundException::new);
            checkOwner(problem, userId);

            ProblemObjective problemObjective = problemObjectiveRepository.findById(prodId).orElseThrow();
            ProblemEditVersion.requireMatch(request.getExpectedEditVersion(),ProblemEditVersion.token(problem,problemObjective,null));
            revisions.capture(problem);

            problem.updateProblem(request.getTitle(), null, request.getDifficulty(), null);
            problemObjective.updateProblemObjective(request.getSummary(), request.getDescription());
            JsonNode next = mutateObo(request.getOboAction(), request.getOboJson(), problemObjective.getOboJsonData());
            problemObjective.updateAssessment(request.getChoices(),request.getAnswer(),request.getIsSeqeuntialAnswer(),OboDocumentValidator.simulation(next)!=null);
            oboValidator.validateQuestion(next, "objective", problemObjective.getAnswer());
            oboValidator.validateChoices(next, problemObjective.getChoices());
            if (request.getOboAction() != null && !"keep".equals(request.getOboAction())) problemObjective.setOboJsonData(next);
            ObjectiveAnswerRules.validate(problemObjective,problemObjective.getAnswer());
            revisions.capture(problem);

            return new ProblemUpdateObjectiveResponse(prodId, problem.getCategory(), problem.getTitle(), problem.getUpatedAt().toString());
    }

    // 코딩형
    @Transactional
    public ProblemUpdateCodingResponse updateProblemCoding(long userId, long probId,
        ProblemUpdateCodingRequest request) {
        Problem problem = problemRepository.findForRevision(probId).orElseThrow(ProblemNotFoundException::new);
        checkOwner(problem, userId);
        ProblemCoding problemCoding = problemCodingRepository.findById(probId).orElseThrow();
        ProblemEditVersion.requireMatch(request.getExpectedEditVersion(),ProblemEditVersion.token(problem,null,problemCoding));
        revisions.capture(problem);

        problem.updateProblem(request.getTitle(), null, request.getDifficulty(), null);
        problemCoding.updateProblemCoding(request.getSummary(), request.getDescription());
        JsonNode next = mutateObo(request.getOboAction(), request.getOboJson(), problemCoding.getOboJsonData());
        if(request.getConstraints()!=null){
            var limits=request.getConstraints();problemCoding.setTimeLimitMs(limits.getTimeLimitMs());
            problemCoding.setMemoryLimitMb(limits.getMemoryLimitMb());problemCoding.setAllowedLanguages(limits.getAllowedLanguages());
        }
        if(request.getTestcases()!=null)problemCoding.setTestcases(request.getTestcases());
        if(request.getInputDescription()!=null)problemCoding.setInputDescription(request.getInputDescription());
        if(request.getOutputDescription()!=null)problemCoding.setOutputDescription(request.getOutputDescription());
        net.diveon.backend.domain.grade.service.CodingExecutionLimits.problem(problemCoding.getTimeLimitMs(),problemCoding.getMemoryLimitMb(),
            problemCoding.getAllowedLanguages(),problemCoding.getTestcases());
        oboValidator.validateQuestion(next, "coding", null);
        oboValidator.validateCodingCases(next, problemCoding.getTestcases());
        if (request.getOboAction() != null && !"keep".equals(request.getOboAction())) problemCoding.setOboJsonData(next);
        revisions.capture(problem);

        return new ProblemUpdateCodingResponse(probId, problem.getType(), problem.getTitle(), problem.getUpatedAt().toString());
    }
    
    // 실습형
    @Transactional
    public ProblemUpdatePracticeResponse updateProblemPractice(long userId, long probId,
        ProblemUpdatePracticeRequest request){
            Problem problem = problemRepository.findById(probId).orElseThrow(ProblemNotFoundException::new);
            checkOwner(problem, userId);
            ProblemPractice problemPractice = problemPracticeRepository.findById(probId).orElseThrow();

            problem.updateProblem(request.getTitle(), null, request.getDifficulty(), null);
            problemPractice.updatePractice(request.getSummary(), request.getDescription());

            return new ProblemUpdatePracticeResponse(probId, problem.getType(), problem.getTitle(), problem.getUpatedAt().toString());
    }
    
    // flag 원문을 암호화해서 저장 - DB에 정답 노출 안 되도록 (실습형)
    private String hashFlag(String flag) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(flag.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }

    private OboStep toOboStep(Problem problem, ForDtoOboStep step) {
        return new OboStep(problem, step.getStep(), step.getDescription(), step.getImageUrl());
    }

    static JsonNode mutateObo(String action, JsonNode supplied, JsonNode current) {
        if (action == null || "keep".equals(action)) return current;
        if ("remove".equals(action)) return null;
        if (!"replace".equals(action) || supplied == null || supplied.isNull())
            throw new net.diveon.backend.global.exception.InvalidSubmissionRequestException("OBO 변경은 keep, replace, remove 중 선택하고 replace에는 자료를 제공하세요.");
        return supplied.deepCopy();
    }
}
