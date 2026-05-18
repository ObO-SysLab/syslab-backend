package net.diveon.backend.domain.contest.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import net.diveon.backend.domain.contest.entity.ContestParticipant;

public interface ContestParticipantRepository extends JpaRepository<ContestParticipant, Long> {
    Optional<ContestParticipant> findByContestIdAndUserId(Long contestId, Long userId);
    long countByContestId(Long contestId);
    long countByContestIdAndScoreGreaterThan(Long contestId, Integer score);
}
