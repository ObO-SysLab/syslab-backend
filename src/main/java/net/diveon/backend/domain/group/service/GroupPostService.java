package net.diveon.backend.domain.group.service;

import net.diveon.backend.domain.group.dto.GroupPostRequest;
import net.diveon.backend.domain.group.dto.GroupPostResponse;
import net.diveon.backend.domain.group.entity.Group;
import net.diveon.backend.domain.group.entity.GroupPost;
import net.diveon.backend.domain.group.entity.GroupPostComment;
import net.diveon.backend.domain.group.entity.GroupUser;
import net.diveon.backend.domain.group.entity.GroupUser.GroupRole;
import net.diveon.backend.domain.group.repository.GroupPostCommentRepository;
import net.diveon.backend.domain.group.repository.GroupPostRepository;
import net.diveon.backend.domain.group.repository.GroupRepository;
import net.diveon.backend.domain.group.repository.GroupUserRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.GroupAccessDeniedException;
import net.diveon.backend.global.exception.GroupNotFoundException;
import net.diveon.backend.global.exception.GroupPostNotFoundException;
import net.diveon.backend.global.exception.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupPostService {

    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;
    private final GroupPostRepository groupPostRepository;
    private final GroupPostCommentRepository groupPostCommentRepository;
    private final UserRepository userRepository;

    public GroupPostService(GroupRepository groupRepository, GroupUserRepository groupUserRepository,
                           GroupPostRepository groupPostRepository,
                           GroupPostCommentRepository groupPostCommentRepository,
                           UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupUserRepository = groupUserRepository;
        this.groupPostRepository = groupPostRepository;
        this.groupPostCommentRepository = groupPostCommentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public GroupPostResponse.PostList getPostList(Long groupId, Long userId, int page, int size) {
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(GroupAccessDeniedException::new);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<GroupPost> posts = groupPostRepository.findByGroup_Id(groupId, pageable);

        return GroupPostResponse.PostList.of(posts);
    }

    @Transactional
    public GroupPostResponse.PostDetailItem getPostDetail(Long groupId, Long postId, Long userId) {
        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(GroupAccessDeniedException::new);

        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(GroupPostNotFoundException::new);

        post.incrementViewCount();

        return GroupPostResponse.PostDetailItem.of(post, userId);
    }

    @Transactional
    public GroupPostResponse.PostCreate createPost(Long groupId, Long userId, GroupPostRequest.Create request) {
        GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(GroupAccessDeniedException::new);

        if ("notice".equals(request.getType()) && groupUser.getRole() != GroupRole.LEADER) {
            throw new GroupAccessDeniedException("공지사항은 그룹장만 작성할 수 있습니다.");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);
        User author = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Boolean isNotice = "notice".equals(request.getType());
        GroupPost post = new GroupPost(group, author, request.getTitle(), request.getContent(), isNotice);
        GroupPost saved = groupPostRepository.save(post);

        return GroupPostResponse.PostCreate.of(saved);
    }

    @Transactional
    public void updatePost(Long groupId, Long postId, Long userId, GroupPostRequest.Update request) {
        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(GroupPostNotFoundException::new);

        if (!post.getAuthor().getId().equals(userId)) {
            throw new GroupAccessDeniedException("본인의 게시글만 수정할 수 있습니다.");
        }

        Boolean isNotice = null;
        if (request.getType() != null) {
            isNotice = "notice".equals(request.getType());
            if (isNotice && !post.getIsNotice()) {
                GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                        .orElseThrow(GroupAccessDeniedException::new);
                if (groupUser.getRole() != GroupRole.LEADER) {
                    throw new GroupAccessDeniedException("공지사항 승격은 그룹장만 가능합니다.");
                }
            }
        }

        post.update(request.getTitle(), request.getContent(), isNotice);
    }

    @Transactional
    public void deletePost(Long groupId, Long postId, Long userId) {
        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(GroupPostNotFoundException::new);

        boolean isAuthor = post.getAuthor().getId().equals(userId);
        if (!isAuthor) {
            GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                    .orElseThrow(GroupAccessDeniedException::new);
            if (groupUser.getRole() != GroupRole.LEADER) {
                throw new GroupAccessDeniedException("본인의 게시글이거나 그룹장만 삭제할 수 있습니다.");
            }
        }

        groupPostRepository.deleteById(postId);
    }
}
