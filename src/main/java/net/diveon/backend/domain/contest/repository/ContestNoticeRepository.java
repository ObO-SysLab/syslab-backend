package net.diveon.backend.domain.contest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import net.diveon.backend.domain.contest.entity.ContestNotice;

public interface ContestNoticeRepository extends JpaRepository<ContestNotice, Long> {
}
