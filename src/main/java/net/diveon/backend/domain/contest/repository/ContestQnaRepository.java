package net.diveon.backend.domain.contest.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import net.diveon.backend.domain.contest.entity.ContestQna;

public interface ContestQnaRepository extends JpaRepository<ContestQna, Long> {
    List<ContestQna> findAllByContestIdOrderByCreatedAtDesc(Long contestId);
    Optional<ContestQna> findByIdAndContestId(Long id, Long contestId);
}
