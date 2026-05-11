package net.diveon.backend.domain.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.group.entity.GroupPostComment;

public class GroupCommentResponse {

    public static class CommentCreate {
        @JsonProperty("comment_id")
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
