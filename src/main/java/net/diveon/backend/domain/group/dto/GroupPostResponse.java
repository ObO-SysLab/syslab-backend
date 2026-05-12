package net.diveon.backend.domain.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.group.entity.GroupPost;
import net.diveon.backend.domain.group.entity.GroupPostComment;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GroupPostResponse {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static class PostList {
        private final long totalElements;
        private final int totalPages;
        private final int currentPage;
        private final List<PostListItem> posts;

        public PostList(long totalElements, int totalPages, int currentPage, List<PostListItem> posts) {
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.currentPage = currentPage;
            this.posts = posts;
        }

        public long getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
        public int getCurrentPage() { return currentPage; }
        public List<PostListItem> getPosts() { return posts; }

        public static PostList of(Page<GroupPost> page) {
            List<PostListItem> posts = page.getContent().stream()
                    .map(PostListItem::of)
                    .collect(Collectors.toList());
            return new PostList(page.getTotalElements(), page.getTotalPages(), page.getNumber() + 1, posts);
        }
    }

    public static class PostListItem {
        private final Long postId;
        private final String type;
        private final String title;
        private final String author;
        private final Integer viewCount;
        private final String createdAt;

        public PostListItem(Long postId, String type, String title, String author, Integer viewCount, String createdAt) {
            this.postId = postId;
            this.type = type;
            this.title = title;
            this.author = author;
            this.viewCount = viewCount;
            this.createdAt = createdAt;
        }

        public Long getPostId() { return postId; }
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public Integer getViewCount() { return viewCount; }
        public String getCreatedAt() { return createdAt; }

        public static PostListItem of(GroupPost post) {
            String type = post.getIsNotice() ? "notice" : "general";
            String createdAtStr = post.getCreatedAt().format(DATE_FORMATTER);
            return new PostListItem(post.getId(), type, post.getTitle(),
                    post.getAuthor().getNickname(), post.getViewCount(), createdAtStr);
        }
    }

    public static class PostDetailItem {
        private final Long postId;
        private final String type;
        private final String title;
        private final String content;
        private final String author;
        private final String createdAt;
        @JsonProperty("isAuthor")
        private final boolean isAuthor;
        private final List<CommentItem> comments;

        public PostDetailItem(Long postId, String type, String title, String content, String author,
                             String createdAt, boolean isAuthor, List<CommentItem> comments) {
            this.postId = postId;
            this.type = type;
            this.title = title;
            this.content = content;
            this.author = author;
            this.createdAt = createdAt;
            this.isAuthor = isAuthor;
            this.comments = comments;
        }

        public Long getPostId() { return postId; }
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getAuthor() { return author; }
        public String getCreatedAt() { return createdAt; }
        public boolean getIsAuthor() { return isAuthor; }
        public List<CommentItem> getComments() { return comments; }

        public static PostDetailItem of(GroupPost post, Long userId, List<GroupPostComment> comments) {
            String type = post.getIsNotice() ? "notice" : "general";
            boolean isAuthor = post.getAuthor().getId().equals(userId);
            String createdAtStr = post.getCreatedAt().format(DATE_FORMATTER);
            List<CommentItem> commentItems = comments.stream()
                    .map(c -> CommentItem.of(c, userId))
                    .collect(Collectors.toList());
            return new PostDetailItem(post.getId(), type, post.getTitle(), post.getContent(),
                    post.getAuthor().getNickname(), createdAtStr, isAuthor, commentItems);
        }
    }

    public static class CommentItem {
        private final Long commentId;
        private final String author;
        private final String content;
        private final String createdAt;
        @JsonProperty("isAuthor")
        private final boolean isAuthor;

        public CommentItem(Long commentId, String author, String content, String createdAt, boolean isAuthor) {
            this.commentId = commentId;
            this.author = author;
            this.content = content;
            this.createdAt = createdAt;
            this.isAuthor = isAuthor;
        }

        public Long getCommentId() { return commentId; }
        public String getAuthor() { return author; }
        public String getContent() { return content; }
        public String getCreatedAt() { return createdAt; }
        public boolean getIsAuthor() { return isAuthor; }

        public static CommentItem of(GroupPostComment comment, Long userId) {
            boolean isAuthor = comment.getAuthor().getId().equals(userId);
            String createdAtStr = comment.getCreatedAt().format(DATE_FORMATTER);
            return new CommentItem(comment.getId(), comment.getAuthor().getNickname(),
                    comment.getContent(), createdAtStr, isAuthor);
        }
    }

    public static class PostCreate {
        @JsonProperty("post_id")
        private final Long postId;

        public PostCreate(Long postId) {
            this.postId = postId;
        }

        public Long getPostId() { return postId; }

        public static PostCreate of(GroupPost post) {
            return new PostCreate(post.getId());
        }
    }
}
