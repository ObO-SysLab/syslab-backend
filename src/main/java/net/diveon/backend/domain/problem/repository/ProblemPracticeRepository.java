package net.diveon.backend.domain.problem.repository;

import net.diveon.backend.domain.problem.entity.ProblemPractice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemPracticeRepository extends JpaRepository<ProblemPractice, Long> {
}
