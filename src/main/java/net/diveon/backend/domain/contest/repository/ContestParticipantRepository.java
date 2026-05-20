package net.diveon.backend.domain.contest.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.diveon.backend.domain.contest.entity.ContestParticipant;

public interface ContestParticipantRepository extends JpaRepository<ContestParticipant, Long> {
    Optional<ContestParticipant> findByContestIdAndUserId(Long contestId, Long userId);
    long countByContestId(Long contestId);
    long countByContestIdAndScoreGreaterThan(Long contestId, Integer score);

    @Query("""
            SELECT cp FROM ContestParticipant cp
            JOIN FETCH cp.user
            WHERE cp.contest.id = :contestId
            ORDER BY cp.score DESC, cp.lastSolvedAt ASC NULLS LAST
            """)
    List<ContestParticipant> findRankingsByContestId(@Param("contestId") Long contestId);

    @Query("""
            SELECT cp FROM ContestParticipant cp
            JOIN FETCH cp.user
            WHERE cp.contest.id = :contestId AND cp.user.id IN :userIds
            """)
    List<ContestParticipant> findByContestIdAndUserIds(
            @Param("contestId") Long contestId,
            @Param("userIds") List<Long> userIds);
}
