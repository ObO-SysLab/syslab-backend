package net.diveon.backend.domain.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.diveon.backend.domain.problem.entity.ProblemComment;

import java.util.List;

public class ProblemCommentResponse {

    public static class CommentList {

        private long total;
        private int page;
        private int size;
        private List<CommentListItem> comments;

        public CommentList(long total, int page, int size, List<CommentListItem> comments) {
            this.total = total;
            this.page = page;
            this.size = size;
            this.comments = comments;
        }

        public long getTotal() { return total; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public List<CommentListItem> getComments() { return comments; }
    }

    // 댓글 목록 조회용 (replies 없음)
    public static class CommentListItem {

        @JsonProperty("commentId")
        private Long commentId;

        @JsonProperty("authorNickname")
        private String authorNickname;

        private String content;

        @JsonProperty("isPrivate")
        private Boolean isPrivate;

        @JsonProperty("createdAt")
        private String createdAt;

        @JsonProperty("updatedAt")
        private String updatedAt;

        public CommentListItem(Long commentId, String authorNickname, String content,
                               Boolean isPrivate, String createdAt, String updatedAt) {
            this.commentId = commentId;
            this.authorNickname = authorNickname;
            this.content = content;
            this.isPrivate = isPrivate;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public static CommentListItem of(ProblemComment comment) {
            return new CommentListItem(
                comment.getId(),
                comment.getAuthor().getNickname(),
                comment.getContent(),
                comment.getIsPrivate(),
                comment.getCreatedAt().toString(),
                comment.getUpdatedAt().toString()
            );
        }

        public Long getCommentId() { return commentId; }
        public String getAuthorNickname() { return authorNickname; }
        public String getContent() { return content; }
        public Boolean getIsPrivate() { return isPrivate; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
    }

    // 댓글 상세 조회용 (replies 있음, 원댓글 + 그 아래 답글 목록을 같이 담는 컨테이너)
    public static class CommentItem {

        @JsonProperty("commentId")
        private Long commentId;

        @JsonProperty("authorNickname")
        private String authorNickname;

        private String content;

        @JsonProperty("isPrivate")
        private Boolean isPrivate;

        @JsonProperty("createdAt")
        private String createdAt;

        @JsonProperty("updatedAt")
        private String updatedAt;

        private List<ReplyItem> replies;

        public CommentItem(Long commentId, String authorNickname, String content,
                           Boolean isPrivate, String createdAt, String updatedAt,
                           List<ReplyItem> replies) {
            this.commentId = commentId;
            this.authorNickname = authorNickname;
            this.content = content;
            this.isPrivate = isPrivate;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.replies = replies;
        }

        public static CommentItem of(ProblemComment comment, List<ReplyItem> replies) {
            return new CommentItem(
                comment.getId(),
                comment.getAuthor().getNickname(),
                comment.getContent(),
                comment.getIsPrivate(),
                comment.getCreatedAt().toString(),
                comment.getUpdatedAt().toString(),
                replies
            );
        }

        public Long getCommentId() { return commentId; }
        public String getAuthorNickname() { return authorNickname; }
        public String getContent() { return content; }
        public Boolean getIsPrivate() { return isPrivate; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public List<ReplyItem> getReplies() { return replies; }
    }


    // 답글 하나짜리 데이터
    public static class ReplyItem {

        @JsonProperty("commentId")
        private Long commentId;

        @JsonProperty("authorNickname")
        private String authorNickname;

        private String content;

        @JsonProperty("isPrivate")
        private Boolean isPrivate;

        @JsonProperty("createdAt")
        private String createdAt;

        @JsonProperty("updatedAt")
        private String updatedAt;

        public ReplyItem(Long commentId, String authorNickname, String content,
                         Boolean isPrivate, String createdAt, String updatedAt) {
            this.commentId = commentId;
            this.authorNickname = authorNickname;
            this.content = content;
            this.isPrivate = isPrivate;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public static ReplyItem of(ProblemComment reply) {
            return new ReplyItem(
                reply.getId(),
                reply.getAuthor().getNickname(),
                reply.getContent(),
                reply.getIsPrivate(),
                reply.getCreatedAt().toString(),
                reply.getUpdatedAt().toString()
            );
        }

        public Long getCommentId() { return commentId; }
        public String getAuthorNickname() { return authorNickname; }
        public String getContent() { return content; }
        public Boolean getIsPrivate() { return isPrivate; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
    }

    // 댓글, 답글 생성
    public static class CommentCreate {

        @JsonProperty("commentId")
        private Long commentId;

        @JsonProperty("probId")
        private Long probId;

        @JsonProperty("authorNickname")
        private String authorNickname;

        private String content;

        @JsonProperty("createdAt")
        private String createdAt;

        public CommentCreate(Long commentId, Long probId, String authorNickname,
                             String content, String createdAt) {
            this.commentId = commentId;
            this.probId = probId;
            this.authorNickname = authorNickname;
            this.content = content;
            this.createdAt = createdAt;
        }

        public static CommentCreate of(ProblemComment comment) {
            return new CommentCreate(
                comment.getId(),
                comment.getProblem().getId(),
                comment.getAuthor().getNickname(),
                comment.getContent(),
                comment.getCreatedAt().toString()
            );
        }

        public Long getCommentId() { return commentId; }
        public Long getProbId() { return probId; }
        public String getAuthorNickname() { return authorNickname; }
        public String getContent() { return content; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class CommentUpdate {

        @JsonProperty("commentId")
        private Long commentId;

        private String content;

        @JsonProperty("isPrivate")
        private Boolean isPrivate;

        @JsonProperty("updatedAt")
        private String updatedAt;

        public CommentUpdate(Long commentId, String content, Boolean isPrivate, String updatedAt) {
            this.commentId = commentId;
            this.content = content;
            this.isPrivate = isPrivate;
            this.updatedAt = updatedAt;
        }

        public static CommentUpdate of(ProblemComment comment) {
            return new CommentUpdate(
                comment.getId(),
                comment.getContent(),
                comment.getIsPrivate(),
                comment.getUpdatedAt().toString()
            );
        }

        public Long getCommentId() { return commentId; }
        public String getContent() { return content; }
        public Boolean getIsPrivate() { return isPrivate; }
        public String getUpdatedAt() { return updatedAt; }
    }

    public static class CommentDelete {

        @JsonProperty("commentId")
        private Long commentId;

        public CommentDelete(Long commentId) {
            this.commentId = commentId;
        }

        public Long getCommentId() { return commentId; }
    }
}
