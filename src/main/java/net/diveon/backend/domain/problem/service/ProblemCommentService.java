package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.notification.service.CommentNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import net.diveon.backend.domain.problem.dto.request.ProblemCommentCreateRequest;
import net.diveon.backend.domain.problem.dto.request.ProblemCommentUpdateRequest;
import net.diveon.backend.domain.problem.dto.response.ProblemCommentResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemComment;
import net.diveon.backend.domain.problem.repository.ProblemCommentRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.CommentAccessDeniedException;
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
    private final ProblemAccessService access;
    private final CommentNotifier notifier;
    private static final Logger log = LoggerFactory.getLogger(ProblemCommentService.class);

    public ProblemCommentService(
        ProblemCommentRepository commentRepository,
        ProblemRepository problemRepository,
        UserRepository userRepository,
        ProblemAccessService access
    ) {
        this(commentRepository, problemRepository, userRepository, access, CommentNotifier.NONE);
    }

    @Autowired
    public ProblemCommentService(
        ProblemCommentRepository commentRepository,
        ProblemRepository problemRepository,
        UserRepository userRepository,
        ProblemAccessService access,
        CommentNotifier notifier
    ) {
        this.commentRepository = commentRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.access = access;
        this.notifier = notifier;
    }

    // 댓글 목록 조회
    // readOnly = true: DB 읽기만 하는 메서드. 변경 감지 안 해서 성능 최적화
    @Transactional(readOnly = true)
    public ProblemCommentResponse.CommentList getComments(long userId, long probId, int page, int size) {
        access.requireRead(userId, probId);

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
        access.requireRead(userId, probId);
        ProblemComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CommentNotFoundException(commentId + "번에 해당하는 댓글이 존재하지 않습니다."));
        if (!comment.getProblem().getId().equals(probId)) throw new CommentNotFoundException();

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
        Problem problem = access.requireRead(userId, probId);

        ProblemComment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                .orElseThrow(() -> new CommentNotFoundException(request.getParentId() + "번에 해당하는 댓글이 존재하지 않습니다."));
            if (!parent.getProblem().getId().equals(probId)) throw new CommentNotFoundException();
        }

        ProblemComment comment = new ProblemComment(problem, user, parent, request.getContent(), request.getIsPrivate());
        ProblemComment saved = commentRepository.save(comment);
        notifyComment(problem, parent, saved, user);

        return ProblemCommentResponse.CommentCreate.of(saved);
    }

    // 최상위 댓글은 출제자에게, 답글은 원 댓글 작성자에게 알린다. 비공개 답글은 원 댓글 작성자가 볼 수 없으면 알리지 않는다.
    // 알림은 별도 트랜잭션이라 실패해도 댓글 저장에는 영향이 없다.
    private void notifyComment(Problem problem, ProblemComment parent, ProblemComment saved, User actor) {
        try {
            long problemAuthorId = problem.getAuthor().getId();
            long recipientId = parent == null ? problemAuthorId : parent.getAuthor().getId();
            boolean visible = parent == null || !Boolean.TRUE.equals(saved.getIsPrivate()) || recipientId == problemAuthorId;
            if (!visible) return;
            notifier.problemComment(recipientId, actor.getId(), actor.getNickname(), saved.getId(),
                    problem.getId(), problem.getTitle(), parent != null);
        } catch (RuntimeException e) {
            log.warn("댓글 알림 실패: {}", e.getMessage());
        }
    }

    // 댓글 수정
    @Transactional
    public ProblemCommentResponse.CommentUpdate updateComment(long userId, long probId, long commentId, ProblemCommentUpdateRequest request) {
        ProblemComment comment = findOwnedComment(userId, probId, commentId);

        comment.updateComment(request.getContent(), request.getIsPrivate());

        return ProblemCommentResponse.CommentUpdate.of(comment);
    }

    // 댓글 삭제
    @Transactional
    public ProblemCommentResponse.CommentDelete deleteComment(long userId, long probId, long commentId) {
        ProblemComment comment = findOwnedComment(userId, probId, commentId);

        commentRepository.delete(comment);

        return new ProblemCommentResponse.CommentDelete(commentId);
    }

    // 댓글이 해당 문제의 것이고 요청자가 작성자인지 확인한 뒤 반환
    private ProblemComment findOwnedComment(long userId, long probId, long commentId) {
        access.requireRead(userId, probId);
        ProblemComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CommentNotFoundException(commentId + "번에 해당하는 댓글이 존재하지 않습니다."));

        if (!comment.getProblem().getId().equals(probId)) {
            throw new CommentNotFoundException(commentId + "번에 해당하는 댓글이 존재하지 않습니다.");
        }

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new CommentAccessDeniedException();
        }

        return comment;
    }
}
