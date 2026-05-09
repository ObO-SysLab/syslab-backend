package net.diveon.backend.domain.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.group.entity.GroupPostComment;

public interface GroupPostCommentRepository extends JpaRepository<GroupPostComment, Long> {
}
