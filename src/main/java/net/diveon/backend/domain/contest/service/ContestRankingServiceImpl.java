package net.diveon.backend.domain.contest.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.contest.dto.response.ContestRankingResponse;
import net.diveon.backend.domain.contest.entity.Contest;
import net.diveon.backend.domain.contest.entity.ContestParticipant;
import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.contest.repository.ContestRepository;
import net.diveon.backend.domain.contest.repository.ContestSubmissionRepository;
import net.diveon.backend.global.exception.ContestAccessDeniedException;
import net.diveon.backend.global.exception.ContestNotFoundException;

@Service
@Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
public class ContestRankingServiceImpl implements ContestRankingService {

    private final ContestRepository contestRepository;
    private final ContestParticipantRepository participantRepository;
    private final ContestSubmissionRepository submissionRepository;

    public ContestRankingServiceImpl(
            ContestRepository contestRepository,
            ContestParticipantRepository participantRepository,
            ContestSubmissionRepository submissionRepository,
            StringRedisTemplate redisTemplate) {
        this.contestRepository = contestRepository;
        this.participantRepository = participantRepository;
        this.submissionRepository = submissionRepository;
    }

    @Override
    public ContestRankingResponse getRankings(Long contestId, Long userId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(ContestNotFoundException::new);

        var caller = participantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestAccessDeniedException::new);
        if (!Boolean.FALSE.equals(caller.getIsBanned())) throw new ContestAccessDeniedException();

        boolean isOngoing = contest.getStatus() == Contest.ContestStatus.ONGOING;

        var rankings = getRankingsFromDB(contestId);

        return new ContestRankingResponse(contestId, isOngoing, rankings);
    }

    private List<ContestRankingResponse.RankingEntry> getRankingsFromDB(Long contestId) {
        List<ContestParticipant> participants =
                participantRepository.findRankingsByContestId(contestId);

        Map<Long, Integer> solvedCounts = getSolvedCounts(contestId);

        List<ContestRankingResponse.RankingEntry> rankings = new ArrayList<>();
        int rank = 1;
        for (ContestParticipant p : participants) {
            Long uid = p.getUser().getId();
            rankings.add(new ContestRankingResponse.RankingEntry(
                    rank++,
                    uid,
                    p.getUser().getNickname(),
                    p.getScore(),
                    p.getLastSolvedAt(),
                    solvedCounts.getOrDefault(uid, 0)
            ));
        }
        return rankings;
    }

    private Map<Long, Integer> getSolvedCounts(Long contestId) {
        List<Object[]> results = submissionRepository.countSolvedByUserForContest(contestId);
        return results.stream().collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> ((Long) row[1]).intValue()
        ));
    }
}
