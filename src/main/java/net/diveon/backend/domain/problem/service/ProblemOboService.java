package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.dto.response.OboPassThroughResponse;
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

    public ProblemOboService(
        ProblemRepository problemRepository,
        ProblemObjectiveRepository problemObjectiveRepository,
        ProblemCodingRepository problemCodingRepository
    ) {
        this.problemRepository = problemRepository;
        this.problemObjectiveRepository = problemObjectiveRepository;
        this.problemCodingRepository = problemCodingRepository;
    }

    @Transactional(readOnly = true)
    public OboPassThroughResponse getOboJson(Long problemId) {
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new ProblemNotFoundException(problemId + "번에 해당하는 문제가 존재하지 않습니다."));

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

    private OboPassThroughResponse toResponseOrNull(ProblemObjective problemObjective) {
        if (problemObjective.getNodes() == null
            && problemObjective.getEdges() == null
            && problemObjective.getFrames() == null) {
            return null;
        }
        return new OboPassThroughResponse(
            problemObjective.getNodes(),
            problemObjective.getEdges(),
            problemObjective.getFrames()
        );
    }

    private OboPassThroughResponse toResponseOrNull(ProblemCoding problemCoding) {
        if (problemCoding.getNodes() == null
            && problemCoding.getEdges() == null
            && problemCoding.getFrames() == null) {
            return null;
        }
        return new OboPassThroughResponse(
            problemCoding.getNodes(),
            problemCoding.getEdges(),
            problemCoding.getFrames()
        );
    }
}
