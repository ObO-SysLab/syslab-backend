package net.diveon.backend.domain.contest.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import net.diveon.backend.domain.contest.entity.ContestNotice;

public class NoticeListResponse {

    private List<NoticeItem> notices;

    public NoticeListResponse(List<NoticeItem> notices) {
        this.notices = notices;
    }

    public List<NoticeItem> getNotices() { return notices; }

    public static class NoticeItem {
        private Long id;
        private String title;
        private String content;
        private String createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static NoticeItem of(ContestNotice notice) {
            NoticeItem item = new NoticeItem();
            item.id = notice.getId();
            item.title = notice.getTitle();
            item.content = notice.getContent();
            item.createdBy = notice.getCreatedBy().getNickname();
            item.createdAt = notice.getCreatedAt();
            item.updatedAt = notice.getUpdatedAt();
            return item;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getCreatedBy() { return createdBy; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
    }
}
