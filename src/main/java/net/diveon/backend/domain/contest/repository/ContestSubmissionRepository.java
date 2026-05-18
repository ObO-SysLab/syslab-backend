package net.diveon.backend.domain.contest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import net.diveon.backend.domain.contest.entity.ContestSubmission;

public interface ContestSubmissionRepository extends JpaRepository<ContestSubmission, Long> {
}
