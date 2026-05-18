package net.diveon.backend.domain.contest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import net.diveon.backend.domain.contest.entity.ContestTag;

public interface ContestTagRepository extends JpaRepository<ContestTag, Long> {
}
