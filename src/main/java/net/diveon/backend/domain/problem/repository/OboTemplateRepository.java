package net.diveon.backend.domain.problem.repository;

import net.diveon.backend.domain.problem.entity.OboTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OboTemplateRepository extends JpaRepository<OboTemplate, Long> {
}
