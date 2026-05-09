package net.diveon.backend.domain.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.group.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {
}
