package net.diveon.backend.domain.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.group.entity.GroupTag;

public interface GroupTagRepository extends JpaRepository<GroupTag, Long> {
}
