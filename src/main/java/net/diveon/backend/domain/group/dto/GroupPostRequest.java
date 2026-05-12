package net.diveon.backend.domain.group.dto;

import jakarta.validation.constraints.NotBlank;

public class GroupPostRequest {

    public static class Create {
        @NotBlank(message = "게시글 타입은 필수입니다.")
        private String type;

        @NotBlank(message = "제목은 필수입니다.")
        private String title;

        @NotBlank(message = "내용은 필수입니다.")
        private String content;

        public Create() {
        }

        public Create(String type, String title, String content) {
            this.type = type;
            this.title = title;
            this.content = content;
        }

        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
    }

    public static class Update {
        private String type;
        private String title;
        private String content;

        public Update() {
        }

        public Update(String type, String title, String content) {
            this.type = type;
            this.title = title;
            this.content = content;
        }

        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
    }
}
