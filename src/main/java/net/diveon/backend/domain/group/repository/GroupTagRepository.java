package net.diveon.backend.domain.group.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.diveon.backend.domain.group.entity.GroupTag;

public interface GroupTagRepository extends JpaRepository<GroupTag, Long> {
    List<GroupTag> findAllByGroupId(Long groupId);
    List<GroupTag> findAllByGroupIdIn(List<Long> groupIds);

    @Modifying
    @Query("DELETE FROM GroupTag gt WHERE gt.group.id = :groupId")
    void deleteAllByGroupId(@Param("groupId") Long groupId);
}
