package net.diveon.backend.domain.problem.repository;

import net.diveon.backend.domain.problem.entity.ProblemPractice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemPracticeRepository extends JpaRepository<ProblemPractice, Long> {
    // Service에서의 호출을 받아서 실제 DB에 INSERT SQL 날림. 이는 JPA가 알아서 처리
}
