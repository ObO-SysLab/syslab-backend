package net.diveon.backend.domain.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.group.entity.GroupPostComment;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class GroupCommentResponse {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static class CommentList {
        @JsonProperty("totalElements")
        private final long totalElements;
        private final List<CommentListItem> comments;

        public CommentList(long totalElements, List<CommentListItem> comments) {
            this.totalElements = totalElements;
            this.comments = comments;
        }

        public long getTotalElements() { return totalElements; }
        public List<CommentListItem> getComments() { return comments; }

        public static CommentList of(Page<GroupPostComment> page, Long userId) {
            List<CommentListItem> comments = page.getContent().stream()
                    .map(c -> CommentListItem.of(c, userId))
                    .collect(Collectors.toList());
            return new CommentList(page.getTotalElements(), comments);
        }
    }

    public static class CommentListItem {
        @JsonProperty("commentId")
        private final Long commentId;
        private final String author;
        private final String content;
        private final String createdAt;
        @JsonProperty("isAuthor")
        private final boolean isAuthor;

        public CommentListItem(Long commentId, String author, String content, String createdAt, boolean isAuthor) {
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

        public static CommentListItem of(GroupPostComment comment, Long userId) {
            boolean isAuthor = comment.getAuthor().getId().equals(userId);
            String createdAtStr = comment.getCreatedAt().format(DATE_FORMATTER);
            return new CommentListItem(comment.getId(), comment.getAuthor().getNickname(),
                    comment.getContent(), createdAtStr, isAuthor);
        }
    }

    public static class CommentCreate {
        @JsonProperty("commentId")
        private final Long commentId;

        public CommentCreate(Long commentId) {
            this.commentId = commentId;
        }

        public Long getCommentId() { return commentId; }

        public static CommentCreate of(GroupPostComment comment) {
            return new CommentCreate(comment.getId());
        }
    }
}
