package net.diveon.backend.domain.problem.repository;

import net.diveon.backend.domain.problem.entity.ProblemComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemCommentRepository extends JpaRepository<ProblemComment, Long> {
    // 최상위 댓글만 (parent = null)
    Page<ProblemComment> findByProblem_IdAndParentIsNull(Long probId, Pageable pageable);
    // 특정 댓글들의 답글 한번에 가져오기
    List<ProblemComment> findByParent_IdInOrderByCreatedAtAsc(List<Long> parentIds);
}
