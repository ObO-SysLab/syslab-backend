package net.diveon.backend.domain.grade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.grade.entity.SolveSubmissionObjective;

public interface SolveSubmissionObjectiveRepository extends JpaRepository<SolveSubmissionObjective, Long> {
    //Optional<User> findByLoginId(String loginId);
    // Optional<SolveSubmissionObjective> findBySubmission(long submissionId);
    //위에거는 @MapsId로 해결함
}
