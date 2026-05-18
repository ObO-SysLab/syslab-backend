package net.diveon.backend.domain.contest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import net.diveon.backend.domain.contest.entity.Contest;

public interface ContestRepository extends JpaRepository<Contest, Long> {
}
