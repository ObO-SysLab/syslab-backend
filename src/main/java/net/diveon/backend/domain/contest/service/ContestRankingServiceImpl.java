package net.diveon.backend.domain.contest.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
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
@Transactional(readOnly = true)
public class ContestRankingServiceImpl implements ContestRankingService {

    private final ContestRepository contestRepository;
    private final ContestParticipantRepository participantRepository;
    private final ContestSubmissionRepository submissionRepository;
    private final StringRedisTemplate redisTemplate;

    public ContestRankingServiceImpl(
            ContestRepository contestRepository,
            ContestParticipantRepository participantRepository,
            ContestSubmissionRepository submissionRepository,
            StringRedisTemplate redisTemplate) {
        this.contestRepository = contestRepository;
        this.participantRepository = participantRepository;
        this.submissionRepository = submissionRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ContestRankingResponse getRankings(Long contestId, Long userId) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(ContestNotFoundException::new);

        participantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestAccessDeniedException::new);

        boolean isOngoing = contest.getStatus() == Contest.ContestStatus.ONGOING;

        List<ContestRankingResponse.RankingEntry> rankings;
        if (isOngoing) {
            try {
                rankings = getRankingsFromRedis(contestId);
            } catch (Exception e) {
                rankings = getRankingsFromDB(contestId);
            }
        } else {
            rankings = getRankingsFromDB(contestId);
        }

        return new ContestRankingResponse(contestId, isOngoing, rankings);
    }

    private List<ContestRankingResponse.RankingEntry> getRankingsFromRedis(Long contestId) {
        String key = "scoreboard:" + contestId;
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, 99);

        if (tuples == null || tuples.isEmpty()) {
            return getRankingsFromDB(contestId);
        }

        List<Long> orderedUserIds = tuples.stream()
                .map(t -> Long.parseLong(t.getValue()))
                .collect(Collectors.toList());

        List<ContestParticipant> participants =
                participantRepository.findByContestIdAndUserIds(contestId, orderedUserIds);

        Map<Long, ContestParticipant> participantMap = participants.stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

        Map<Long, Integer> solvedCounts = getSolvedCounts(contestId);

        List<ContestRankingResponse.RankingEntry> rankings = new ArrayList<>();
        int rank = 1;
        for (Long uid : orderedUserIds) {
            ContestParticipant p = participantMap.get(uid);
            if (p == null) continue;
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
