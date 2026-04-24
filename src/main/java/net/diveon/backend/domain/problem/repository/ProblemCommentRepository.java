package net.diveon.backend.domain.problem.repository;

import net.diveon.backend.domain.problem.entity.ProblemComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemCommentRepository extends JpaRepository<ProblemComment, Long> {

    Page<ProblemComment> findByProblem_IdAndParentIsNull(Long probId, Pageable pageable);
    // Page<>는 데이터 목록이랑 총 개수를 한번에 가져와 줌. (List<>로 받으면 count쿼리를 따로 날려야.)
    List<ProblemComment> findByParent_IdIn(List<Long> parentIds);
}
