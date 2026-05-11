package net.diveon.backend.domain.group.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.group.entity.GroupTag;

public interface GroupTagRepository extends JpaRepository<GroupTag, Long> {
    List<GroupTag> findAllByGroupId(Long groupId);
    List<GroupTag> findAllByGroupIdIn(List<Long> groupIds);
}
