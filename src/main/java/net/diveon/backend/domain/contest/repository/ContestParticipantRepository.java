package net.diveon.backend.domain.contest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import net.diveon.backend.domain.contest.entity.ContestParticipant;

public interface ContestParticipantRepository extends JpaRepository<ContestParticipant, Long> {
}
