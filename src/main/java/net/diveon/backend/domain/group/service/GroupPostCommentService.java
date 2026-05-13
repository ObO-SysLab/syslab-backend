package net.diveon.backend.domain.group.service;

import net.diveon.backend.domain.group.dto.GroupCommentRequest;
import net.diveon.backend.domain.group.dto.GroupCommentResponse;
import net.diveon.backend.domain.group.entity.GroupPost;
import net.diveon.backend.domain.group.entity.GroupPostComment;
import net.diveon.backend.domain.group.entity.GroupUser;
import net.diveon.backend.domain.group.entity.GroupUser.GroupRole;
import net.diveon.backend.domain.group.repository.GroupPostCommentRepository;
import net.diveon.backend.domain.group.repository.GroupPostRepository;
import net.diveon.backend.domain.group.repository.GroupUserRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.CommentNotFoundException;
import net.diveon.backend.global.exception.GroupAccessDeniedException;
import net.diveon.backend.global.exception.GroupPostNotFoundException;
import net.diveon.backend.global.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupPostCommentService {

    private final GroupPostRepository groupPostRepository;
    private final GroupPostCommentRepository groupPostCommentRepository;
    private final GroupUserRepository groupUserRepository;
    private final UserRepository userRepository;

    public GroupPostCommentService(GroupPostRepository groupPostRepository,
                                   GroupPostCommentRepository groupPostCommentRepository,
                                   GroupUserRepository groupUserRepository,
                                   UserRepository userRepository) {
        this.groupPostRepository = groupPostRepository;
        this.groupPostCommentRepository = groupPostCommentRepository;
        this.groupUserRepository = groupUserRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GroupCommentResponse.CommentCreate createComment(Long groupId, Long postId, Long userId,
                                                             GroupCommentRequest.Create request) {
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(GroupAccessDeniedException::new);

        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(GroupPostNotFoundException::new);

        User author = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        GroupPostComment comment = new GroupPostComment(post, author, request.getContent());
        GroupPostComment saved = groupPostCommentRepository.save(comment);

        return GroupCommentResponse.CommentCreate.of(saved);
    }

    @Transactional
    public void updateComment(Long groupId, Long postId, Long commentId, Long userId,
                             GroupCommentRequest.Update request) {
        GroupPostComment comment = groupPostCommentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new GroupAccessDeniedException("본인의 댓글만 수정할 수 있습니다.");
        }

        comment.update(request.getContent());
    }

    @Transactional
    public void deleteComment(Long groupId, Long postId, Long commentId, Long userId) {
        GroupPostComment comment = groupPostCommentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        boolean isCommentAuthor = comment.getAuthor().getId().equals(userId);
        if (!isCommentAuthor) {
            GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                    .orElseThrow(GroupAccessDeniedException::new);
            if (groupUser.getRole() != GroupRole.LEADER) {
                throw new GroupAccessDeniedException("본인의 댓글이거나 그룹장만 삭제할 수 있습니다.");
            }
        }

        groupPostCommentRepository.deleteById(commentId);
    }
}
