package net.diveon.backend.domain.grade.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.grade.entity.SolveResult;

public interface SolveResultRepository extends JpaRepository<SolveResult, Long>{

    Optional<SolveResult> findBySubmissionId(Long submissionId);
}
