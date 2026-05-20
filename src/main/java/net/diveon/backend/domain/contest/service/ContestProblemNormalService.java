package net.diveon.backend.domain.contest.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.contest.dto.request.ContestProblemPointsUpdateRequest;
import net.diveon.backend.domain.contest.dto.response.ContestProblemListResponse;
import net.diveon.backend.domain.contest.entity.Contest;
import net.diveon.backend.domain.contest.entity.ContestParticipant;
import net.diveon.backend.domain.contest.entity.ContestProblem;
import net.diveon.backend.domain.contest.others.ForDtoContestProblem;
import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.contest.repository.ContestProblemRepository;
import net.diveon.backend.domain.contest.repository.ContestRepository;
import net.diveon.backend.domain.contest.repository.ContestSubmissionRepository;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.ContestAccessDeniedException;
import net.diveon.backend.global.exception.ContestNotFoundException;
import net.diveon.backend.global.exception.ContestParticipantNotFoundException;
import net.diveon.backend.global.exception.ContestProblemNotFoundException;
import net.diveon.backend.global.exception.InvalidContestProblemPointsException;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import net.diveon.backend.global.exception.UserNotFoundException;

@Service
public class ContestProblemNormalService {

    private final ContestRepository contestRepository;
    private final UserRepository userRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestSubmissionRepository contestSubmissionRepository;
    private final ProblemRepository problemRepository;

    public ContestProblemNormalService(ContestRepository contestRepository,
                                       UserRepository userRepository,
                                       ContestParticipantRepository contestParticipantRepository,
                                       ContestProblemRepository contestProblemRepository,
                                       ContestSubmissionRepository contestSubmissionRepository,
                                       ProblemRepository problemRepository) {
        this.contestRepository = contestRepository;
        this.userRepository = userRepository;
        this.contestParticipantRepository = contestParticipantRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.contestSubmissionRepository = contestSubmissionRepository;
        this.problemRepository = problemRepository;
    }

    @Transactional(readOnly = true)
    public ContestProblemListResponse getContestProblems(Long contestId, Long userId) {
        if (!contestRepository.existsById(contestId)) {
            throw new ContestNotFoundException();
        }

        userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        ContestParticipant participant = contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestParticipantNotFoundException::new);
        if (participant.getIsBanned()) {
            throw new ContestAccessDeniedException();
        }

        List<ContestProblem> contestProblems =
                contestProblemRepository.findAllWithProblemByContestId(contestId);
        Set<Long> solvedContestProblemIds = Set.copyOf(
                contestSubmissionRepository.findSolvedContestProblemIds(contestId, userId));

        List<ForDtoContestProblem> problems = new java.util.ArrayList<>();
        for (int i = 0; i < contestProblems.size(); i++) {
            problems.add(toResponse(contestProblems.get(i), solvedContestProblemIds, i + 1));
        }

        return new ContestProblemListResponse(problems);
    }

    @Transactional
    public void updateContestProblemPoints(Long contestId, Long problemId, Long userId,
                                           ContestProblemPointsUpdateRequest request) {
        if (!problemRepository.existsById(problemId)) {
            throw new ProblemNotFoundException();
        }

        ContestProblem contestProblem = contestProblemRepository.findByContestIdAndProblemId(contestId, problemId)
                .orElseThrow(ContestProblemNotFoundException::new);

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(ContestNotFoundException::new);

        ContestParticipant participant = contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestAccessDeniedException::new);

        boolean isContestCreator = contest.getCreatedBy().getId().equals(userId);
        boolean isContestAdmin = participant.getRole() == ContestParticipant.ContestRole.ADMIN;
        if (!isContestCreator || !isContestAdmin) {
            throw new ContestAccessDeniedException();
        }

        Integer points = request.getPoints();
        if (points == null || points < 0) {
            throw new InvalidContestProblemPointsException();
        }

        contestProblem.updatePoints(points);
    }

    private ForDtoContestProblem toResponse(ContestProblem contestProblem, Set<Long> solvedContestProblemIds, int sequence) {
        Problem problem = contestProblem.getProblem();

        /*
         * 현재 대회 문제는 신규 생성되고 Problem.visibility 값이 "contest"인 전용 문제만 사용하므로
         * solvedCount는 Problem.solvedCount를 그대로 사용한다.
         *
         * 추후 기존 문제를 대회에 추가할 수 있게 되면 Problem.solvedCount는 전체 풀이 수가 되므로,
         * ContestSubmission에서 contest_problem_id별 is_correct = true인 distinct user_id 수를
         * 집계하는 방식으로 확장해야 한다.
         */
        return new ForDtoContestProblem(
                buildDisplayId(problem.getCategory(), sequence),
                problem.getId(),
                problem.getTitle(),
                contestProblem.getPoints(),
                problem.getSolvedCount(),
                problem.getCategory(),
                solvedContestProblemIds.contains(contestProblem.getId())
        );
    }

    private String buildDisplayId(String category, int sequence) {
        String prefix = "Problem";
        if (category != null && !category.isBlank()) {
            prefix = category;
        }
        return String.format("%s-%02d", prefix, sequence);
    }
}
