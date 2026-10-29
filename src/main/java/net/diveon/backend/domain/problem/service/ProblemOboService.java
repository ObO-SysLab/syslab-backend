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

    public ProblemOboService(
        ProblemRepository problemRepository,
        ProblemObjectiveRepository problemObjectiveRepository,
        ProblemCodingRepository problemCodingRepository,
        ProblemAccessService access
    ) {
        this.problemRepository = problemRepository;
        this.problemObjectiveRepository = problemObjectiveRepository;
        this.problemCodingRepository = problemCodingRepository;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public JsonNode getOboJson(Long userId, Long problemId) {
        Problem problem = access.requireRead(userId, problemId);

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
