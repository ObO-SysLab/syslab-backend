package net.diveon.backend.domain.group.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.group.entity.GroupPost;

public interface GroupPostRepository extends JpaRepository<GroupPost, Long> {
    Page<GroupPost> findByGroup_Id(Long groupId, Pageable pageable);
}
