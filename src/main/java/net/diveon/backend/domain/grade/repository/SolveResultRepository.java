package net.diveon.backend.domain.grade.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.grade.entity.SovleResult;

public interface SolveResultRepository extends JpaRepository<SovleResult, Long>{

    Optional<SovleResult> findBySubmissionId(Long submissionId);
}
