package net.diveon.backend.domain.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.group.entity.GroupAssignRequest;

public interface GroupAssignRequestRepository extends JpaRepository<GroupAssignRequest, Long> {
}
