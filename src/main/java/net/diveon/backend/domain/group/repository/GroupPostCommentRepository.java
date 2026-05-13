package net.diveon.backend.domain.group.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.group.entity.GroupPostComment;

public interface GroupPostCommentRepository extends JpaRepository<GroupPostComment, Long> {
    List<GroupPostComment> findAllByPost_Id(Long postId);
    Page<GroupPostComment> findAllByPost_IdOrderByCreatedAtDesc(Long postId, Pageable pageable);
}
