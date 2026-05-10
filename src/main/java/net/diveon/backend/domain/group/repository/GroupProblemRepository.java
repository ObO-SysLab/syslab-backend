package net.diveon.backend.domain.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.group.entity.GroupProblem;

public interface GroupProblemRepository extends JpaRepository<GroupProblem, Long> {
    long countByGroupId(Long groupId);
    boolean existsByGroupIdAndProblemId(Long groupId, Long problemId);
}
