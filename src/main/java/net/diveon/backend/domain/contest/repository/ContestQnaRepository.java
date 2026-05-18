package net.diveon.backend.domain.contest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import net.diveon.backend.domain.contest.entity.ContestQna;

public interface ContestQnaRepository extends JpaRepository<ContestQna, Long> {
}
