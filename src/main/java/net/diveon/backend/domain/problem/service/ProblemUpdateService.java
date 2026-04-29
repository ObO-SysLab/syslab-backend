package net.diveon.backend.domain.problem.service;

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

    public ProblemUpdateService(ProblemRepository problemRepository,
        ProblemObjectiveRepository problemObjectiveRepository,
        ProblemPracticeRepository problemPracticeRepository,
        ProblemCodingRepository problemCodingRepository,
        OboStepRepository oboStepRepository,
        UserRepository userRepository){
            this.problemObjectiveRepository = problemObjectiveRepository;
            this.problemPracticeRepository = problemPracticeRepository;
            this.problemCodingRepository = problemCodingRepository;
            this.problemRepository = problemRepository;
            this.oboStepRepository = oboStepRepository;
            this.userRepository = userRepository;
        }

    // 객관식형
    @Transactional
    public ProblemUpdateObjectiveResponse updateProblemObjective(long userId, long prodId, 
        ProblemUpdateObjectiveRequest request){
            Problem problem = problemRepository.findById(prodId).orElseThrow();

            ProblemObjective problemObjective = problemObjectiveRepository.findById(prodId).orElseThrow();

            problem.updateProblem(request.getTitle(), request.getCategory(), request.getDifficulty(), request.getVisibility());

            problemObjective.updateProblemObjective(
                request.getSummary(),
                request.getDescription(),
                request.getChoices(),
                request.getAnswer(),
                request.getOboEnabled()
            );

            // List<OboStep> existingSteps = oboStepRepository.findByProblemOrderByStepAsc(problem);
            // if (!existingSteps.isEmpty()) {
            //     oboStepRepository.deleteAll(existingSteps);
            // }

            //이 아래의 로직은, obo step에 대해서, 일단 전부 삭제하고 전부 삽입하는것이라, 
            // update에서 아무 값도 안주면 바로 다 삭제되는 방식입니다.


            /**
             * TODO : 현재 객관식 업데이트에서 obo step을 업데이트 하는 방법을 고도화 하기
             * 고도화 하는 방법은 다음과 같음
             * 1. 현재 삭제 방식을 다듬어서, 값이 주어지지 않는다면, 삭제가 되지 않도록 하기
             * 2. 개별 삭제 기능을 구현할 것.
             */
            oboStepRepository.deleteByProblem_Id(prodId);

            // 개별 업데이트 개선 필요
            if (request.getObo() != null && request.getObo().getSteps() != null && !request.getObo().getSteps().isEmpty()) {
                List<OboStep> oboSteps = request.getObo().getSteps().stream()
                    .map(step -> toOboStep(problem, step))
                    .toList();
                oboStepRepository.saveAll(oboSteps);
            }

            return new ProblemUpdateObjectiveResponse(prodId, request.getCategory(), request.getTitle(), problem.getUpatedAt().toString());
    }

    // 코딩형
    @Transactional
    public ProblemUpdateCodingResponse updateProblemCoding(long userId, long probId,
        ProblemUpdateCodingRequest request) {
        Problem problem = problemRepository.findById(probId).orElseThrow();
        ProblemCoding problemCoding = problemCodingRepository.findById(probId).orElseThrow();

        problem.updateProblem(request.getTitle(), request.getCategory(), request.getDifficulty(), request.getVisibility());

        ProblemUpdateCodingRequest.Constraints constraints = request.getConstraints();
        ProblemUpdateCodingRequest.Obo obo = request.getObo();

        problemCoding.updateProblemCoding(
            request.getSummary(),
            request.getDescription(),
            request.getInputDescription(),
            request.getOutputDescription(),
            constraints != null ? constraints.getTimeLimitMs() : null,
            constraints != null ? constraints.getMemoryLimitMb() : null,
            constraints != null ? constraints.getAllowedLanguages() : null,
            request.getTestcases(),
            request.getFileUrl(),
            obo != null ? obo.getEnabled() : null,
            obo != null ? obo.getInitialImageUrl() : null
        );

        return new ProblemUpdateCodingResponse(
            probId,
            problem.getType(),
            problem.getTitle(),
            problem.getUpatedAt().toString()
        );
    }
    
    // 실습형
    @Transactional
    public ProblemUpdatePracticeResponse updateProblemPractice(long userId, long probId,
        ProblemUpdatePracticeRequest request){
            Problem problem = problemRepository.findById(probId).orElseThrow();
            ProblemPractice problemPractice = problemPracticeRepository.findById(probId).orElseThrow();

            problem.updateProblem(
                request.getTitle(),
                request.getCategory(),
                request.getDifficulty(),
                request.getVisibility()
            );

            String flagHash = request.getFlag() != null ? hashFlag(request.getFlag()) : null;
            ProblemUpdatePracticeRequest.VmConfig vmConfig = request.getVmConfig();

            problemPractice.updatePractice(
                request.getSummary(),
                request.getDescription(),
                vmConfig != null ? vmConfig.getOsImage() : null,
                vmConfig != null ? vmConfig.getAllowedCommands() : null,
                vmConfig != null ? vmConfig.getCpuLimit() : null,
                vmConfig != null ? vmConfig.getMemoryLimit() : null,
                flagHash,
                request.getDockerFileUrl()
            );

            return new ProblemUpdatePracticeResponse(
                probId,
                problem.getType(),
                problem.getTitle(),
                problem.getUpatedAt().toString()
            );
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
}
