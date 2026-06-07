package net.diveon.backend.domain.contest.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        Map<Long, Integer> submittedCountMap = toCountMap(
                contestSubmissionRepository.countSubmissionsPerProblem(contestId));
        Map<Long, Integer> solvedCountMap = toCountMap(
                contestSubmissionRepository.countSolvedPerProblem(contestId));

        List<ForDtoContestProblem> problems = new java.util.ArrayList<>();
        for (int i = 0; i < contestProblems.size(); i++) {
            problems.add(toResponse(contestProblems.get(i), solvedContestProblemIds,
                    submittedCountMap, solvedCountMap, i + 1));
        }

        return new ContestProblemListResponse(problems);
    }

    @Transactional
    public void deleteContestProblem(Long contestId, Long problemId, Long userId) {
        userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (!problemRepository.existsById(problemId)) {
            throw new ProblemNotFoundException();
        }

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(ContestNotFoundException::new);

        ContestProblem contestProblem = contestProblemRepository.findByContestIdAndProblemId(contestId, problemId)
                .orElseThrow(ContestProblemNotFoundException::new);

        ContestParticipant participant = contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestAccessDeniedException::new);

        boolean isContestCreator = contest.getCreatedBy().getId().equals(userId);
        boolean isContestAdmin = participant.getRole() == ContestParticipant.ContestRole.ADMIN;
        if (!isContestCreator || !isContestAdmin) {
            throw new ContestAccessDeniedException();
        }

        Problem problem = contestProblem.getProblem();
        contestProblemRepository.delete(contestProblem);

        if (contest.getStatus() == Contest.ContestStatus.UPCOMING && "contest".equals(problem.getVisibility())) {
            problemRepository.delete(problem);
        }
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

    @Transactional
    public void addProblemToContestServiceLogic(Long contestId, Long problemId, Long requesterId, Integer point){

        // 보호로직
        /**
         * 해당 문제 자체가 없는 경우
         * 해당 유저 자체가 없는 경우
         * 해당 대회 자체가 없는 경우
         *
         * 해당 유저가 대회 참여자 + 어드민이 아닌경우
         * 해당 문제가 요청자의 문제가 아닌경우
         *
         */

        //문제 자체가 없는경우
        Problem problem = problemRepository.findById(problemId)
        .orElseThrow(ProblemNotFoundException::new);

        //유저 자체가 없는 경우
        if (!userRepository.existsById(requesterId)) {
            throw new UserNotFoundException();
        }

        //대회 자체가 없는 경우
        Contest contest = contestRepository.findById(contestId)
        .orElseThrow(ContestNotFoundException::new);

        ContestParticipant contestParticipantAdmin = contestParticipantRepository.findByContestIdAndUserId(contestId, requesterId)
        .orElseThrow(ContestParticipantNotFoundException::new);

        // 대회 어드민이 아닌 경우
        boolean isContestCreator = contest.getCreatedBy().getId().equals(requesterId);
        boolean isContestAdmin = contestParticipantAdmin.getRole() == ContestParticipant.ContestRole.ADMIN;
        if (!isContestCreator || !isContestAdmin) {
            throw new ContestAccessDeniedException();
        }

        if (!problem.getAuthor().getId().equals(requesterId)) {
            throw new ProblemNotFoundException("해당 문제의 소유자가 아닙니다.");
        }

        ContestProblem contestProblem = new ContestProblem(contest, problem, point);
        contestProblemRepository.save(contestProblem);
    }

    private ForDtoContestProblem toResponse(ContestProblem contestProblem, Set<Long> solvedContestProblemIds,
                                             Map<Long, Integer> submittedCountMap, Map<Long, Integer> solvedCountMap,
                                             int sequence) {
        Problem problem = contestProblem.getProblem();
        Long cpId = contestProblem.getId();
        return new ForDtoContestProblem(
                buildDisplayId(problem.getCategory(), sequence),
                cpId,
                problem.getId(),
                problem.getTitle(),
                contestProblem.getPoints(),
                submittedCountMap.getOrDefault(cpId, 0),
                solvedCountMap.getOrDefault(cpId, 0),
                problem.getCategory(),
                solvedContestProblemIds.contains(cpId)
        );
    }

    private Map<Long, Integer> toCountMap(List<Object[]> rows) {
        Map<Long, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return map;
    }

    private String buildDisplayId(String category, int sequence) {
        String prefix = "Problem";
        if (category != null && !category.isBlank()) {
            prefix = category;
        }
        return String.format("%s-%02d", prefix, sequence);
    }
}
