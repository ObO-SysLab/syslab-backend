package net.diveon.backend.domain.group.dto;

import jakarta.validation.constraints.NotBlank;

public class GroupCommentRequest {

    public static class Create {
        @NotBlank(message = "댓글 내용은 필수입니다.")
        private String content;

        public Create() {
        }

        public Create(String content) {
            this.content = content;
        }

        public String getContent() { return content; }
    }

    public static class Update {
        @NotBlank(message = "댓글 내용은 필수입니다.")
        private String content;

        public Update() {
        }

        public Update(String content) {
            this.content = content;
        }

        public String getContent() { return content; }
    }
}
