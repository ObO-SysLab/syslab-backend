package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.dto.request.ProblemCommentCreateRequest;
import net.diveon.backend.domain.problem.dto.request.ProblemCommentUpdateRequest;
import net.diveon.backend.domain.problem.dto.response.ProblemCommentResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemComment;
import net.diveon.backend.domain.problem.repository.ProblemCommentRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.CommentNotFoundException;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProblemCommentService {

    private final ProblemCommentRepository commentRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;

    public ProblemCommentService(
        ProblemCommentRepository commentRepository,
        ProblemRepository problemRepository,
        UserRepository userRepository
    ) {
        this.commentRepository = commentRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
    }

    // 댓글 목록 조회
    // readOnly = true: DB 읽기만 하는 메서드. 변경 감지 안 해서 성능 최적화
    @Transactional(readOnly = true)
    public ProblemCommentResponse.CommentList getComments(long userId, long probId, int page, int size) {
        problemRepository.findById(probId)
            .orElseThrow(() -> new ProblemNotFoundException(probId + "번에 해당하는 문제가 존재하지 않습니다."));

        // 최상위 댓글만 (parent = null) 페이지 단위로 가져옴. page-1인 이유: JPA는 페이지 0부터 시작
        Page<ProblemComment> commentPage = commentRepository.findByProblem_IdAndParentIsNull(probId, PageRequest.of(page - 1, size, Sort.by("createdAt").descending()));

        // 댓글 하나씩 돌면서 CommentListItem DTO로 변환 (replies 없음)
        List<ProblemCommentResponse.CommentListItem> comments = commentPage.getContent().stream()
            .map(comment -> ProblemCommentResponse.CommentListItem.of(comment))
            .toList();

        // 총 댓글 수, 페이지, 사이즈, 댓글 목록 묶어서 반환
        return new ProblemCommentResponse.CommentList(
            commentPage.getTotalElements(),
            page,
            size,
            comments
        );
    }

    // 댓글 상세 조회
    @Transactional(readOnly = true)
    public ProblemCommentResponse.CommentItem getComment(long userId, long probId, long commentId) {
        ProblemComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CommentNotFoundException(commentId + "번에 해당하는 댓글이 존재하지 않습니다."));

        List<ProblemCommentResponse.ReplyItem> replies = commentRepository
            .findByParent_IdInOrderByCreatedAtAsc(List.of(commentId))
            .stream()
            .map(reply -> ProblemCommentResponse.ReplyItem.of(reply))
            .toList();

        return ProblemCommentResponse.CommentItem.of(comment, replies);
    }

    // 댓글 생성
    @Transactional
    public ProblemCommentResponse.CommentCreate createComment(long userId, long probId, ProblemCommentCreateRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        Problem problem = problemRepository.findById(probId)
            .orElseThrow(() -> new ProblemNotFoundException(probId + "번에 해당하는 문제가 존재하지 않습니다."));

        ProblemComment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                .orElseThrow(() -> new CommentNotFoundException(request.getParentId() + "번에 해당하는 댓글이 존재하지 않습니다."));
        }

        ProblemComment comment = new ProblemComment(problem, user, parent, request.getContent(), request.getIsPrivate());
        ProblemComment saved = commentRepository.save(comment);

        return ProblemCommentResponse.CommentCreate.of(saved);
    }

    // 댓글 수정
    @Transactional
    public ProblemCommentResponse.CommentUpdate updateComment(long userId, long probId, long commentId, ProblemCommentUpdateRequest request) {
        ProblemComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CommentNotFoundException(commentId + "번에 해당하는 댓글이 존재하지 않습니다."));

        comment.updateComment(request.getContent(), request.getIsPrivate());

        return ProblemCommentResponse.CommentUpdate.of(comment);
    }

    // 댓글 삭제
    @Transactional
    public ProblemCommentResponse.CommentDelete deleteComment(long userId, long probId, long commentId) {
        ProblemComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CommentNotFoundException(commentId + "번에 해당하는 댓글이 존재하지 않습니다."));

        commentRepository.delete(comment);

        return new ProblemCommentResponse.CommentDelete(commentId);
    }
}
