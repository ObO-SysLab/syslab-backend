package net.diveon.backend.domain.problem.repository;
import net.diveon.backend.domain.problem.entity.ProblemRevision;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProblemRevisionRepository extends JpaRepository<ProblemRevision,String> {}
