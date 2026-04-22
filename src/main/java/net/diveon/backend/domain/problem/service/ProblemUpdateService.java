package net.diveon.backend.domain.problem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.problem.dto.request.ProblemUpdateObjectiveRequest;
import net.diveon.backend.domain.problem.dto.request.ProblemUpdatePracticeRequest;
import net.diveon.backend.domain.problem.dto.response.ProblemCreateObjectiveResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemUpdateObjectiveResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemUpdatePracticeResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.problem.entity.ProblemPractice;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.repository.UserRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;


@Service
public class ProblemUpdateService {
    private final ProblemRepository problemRepository;
    private final ProblemObjectiveRepository problemObjectiveRepository;
    private final ProblemPracticeRepository problemPracticeRepository;
    private final UserRepository userRepository;

    public ProblemUpdateService(ProblemRepository problemRepository,
        ProblemObjectiveRepository problemObjectiveRepository,
        ProblemPracticeRepository problemPracticeRepository,
        UserRepository userRepository){
            this.problemObjectiveRepository = problemObjectiveRepository;
            this.problemPracticeRepository = problemPracticeRepository;
            this.problemRepository = problemRepository;
            this.userRepository = userRepository;
        }

    // 객관식
    @Transactional
    public ProblemUpdateObjectiveResponse updateProblemObjective(long userId, long prodId, 
        ProblemUpdateObjectiveRequest request){
            Problem problem = problemRepository.findById(prodId).orElseThrow();

            ProblemObjective problemObjective = problemObjectiveRepository.findById(prodId).orElseThrow();

            problem.updateProblem(request.getTitle(), request.getCategory(), request.getDifficulty(), request.getVisibility());

            // 여기서 request.getObo().getEnabled() 으로 넣는데, 이때 null 이면 null.getEnabled() 라고되어서 문제 생김 나중에 수정이 필요함.
            problemObjective.updateProblemObjective(request.getTitle(), request.getDescription(), 
            request.getChoices(), request.getAnswer(), 
            null, null);


            return new ProblemUpdateObjectiveResponse(prodId, request.getCategory(), request.getTitle(), problem.getUpatedAt().toString());
    }

    // 실습형
    @Transactional
    public ProblemUpdatePracticeResponse updateProblemPractice(String userId, long probId,
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

    private String hashFlag(String flag) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(flag.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
