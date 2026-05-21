package net.diveon.backend.domain.contest.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import net.diveon.backend.domain.contest.entity.ContestNotice;

public interface ContestNoticeRepository extends JpaRepository<ContestNotice, Long> {
    List<ContestNotice> findAllByContestIdOrderByCreatedAtDesc(Long contestId);
    Optional<ContestNotice> findByIdAndContestId(Long id, Long contestId);
}
