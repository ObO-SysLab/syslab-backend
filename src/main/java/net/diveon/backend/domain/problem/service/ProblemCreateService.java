package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.dto.request.ProblemCreateObjectiveRequest;
import net.diveon.backend.domain.problem.dto.request.ProblemCreatePracticeRequest;
import net.diveon.backend.domain.problem.dto.response.ProblemCreateObjectiveResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemCreatePracticeResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.problem.entity.ProblemPractice;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class ProblemCreateService {
    private final ProblemObjectiveRepository problemObjectiveRepository;
    private final ProblemPracticeRepository problemPracticeRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;

    public ProblemCreateService(ProblemObjectiveRepository problemObjectiveRepository,
        ProblemPracticeRepository problemPracticeRepository,
        ProblemRepository problemRepository, UserRepository userRepository){
        this.problemObjectiveRepository = problemObjectiveRepository;
        this.problemPracticeRepository = problemPracticeRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
    }

    
    @Transactional
    public ProblemCreateObjectiveResponse createObjective(ProblemCreateObjectiveRequest request, String userId){
        User author = userRepository.findById(Long.parseLong(userId))
                .orElseThrow();

        Problem problem = new Problem(
                author,
                "objective",
                request.getTitle(),
                request.getCategory(),
                request.getDifficulty(),
                request.getVisibility()
        );
        Problem savedProblem = problemRepository.save(problem);

        Boolean oboEnabled = request.getObo() != null ? request.getObo().getEnabled() : false;
        ProblemObjective problemObjective = new ProblemObjective(
                savedProblem,
                request.getTitle(), // 요놈은 summary인데 없어서 일단 가정으로 추가함.
                request.getDescription(),
                request.getChoices(),
                request.getAnswer(),
                oboEnabled,
                request.getObo() != null ? request.getObo().getSteps() : null
        );
        problemObjectiveRepository.save(problemObjective);

        return new ProblemCreateObjectiveResponse(
                savedProblem.getId(),
                savedProblem.getType(),
                savedProblem.getTitle(),
                savedProblem.getCreatedAt().toString()
        );
    }

    // 실습형
    @Transactional
    public ProblemCreatePracticeResponse createPractice(ProblemCreatePracticeRequest request, String userId){
        User author = userRepository.findById(Long.parseLong(userId)) // ex. "1" -> 1 (long)
                .orElseThrow();

        Problem problem = new Problem(
                author,
                "practice",
                request.getTitle(),
                request.getCategory(),
                request.getDifficulty(),
                request.getVisibility()
        );
        Problem savedProblem = problemRepository.save(problem);

        ProblemPractice problemPractice = new ProblemPractice( // Java 메모리에 객체만 만든 것, 서버 꺼지면 사라짐
                savedProblem,
                request.getSummary(),
                request.getDescription(),
                request.getVmConfig().getOsImage(),
                request.getVmConfig().getAllowedCommands(),
                request.getVmConfig().getCpuLimit(),
                request.getVmConfig().getMemoryLimit(),
                hashFlag(request.getFlag()),
                request.getDockerFileUrl()
        );
        problemPracticeRepository.save(problemPractice); // DB에 실제 저장위해 INSERT, 서버 꺼져도 영구 저장

        return new ProblemCreatePracticeResponse( // DB에 저장 완료 후 프론트한테 돌려줄 응답 데이터
                savedProblem.getId(),
                savedProblem.getType(),
                savedProblem.getTitle(),
                savedProblem.getCreatedAt().toString()
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
}
