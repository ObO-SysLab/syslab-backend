package net.diveon.backend.domain.problem.repository;

import net.diveon.backend.domain.problem.entity.ProblemCoding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProblemCodingRepository extends JpaRepository<ProblemCoding, Long> {
}
