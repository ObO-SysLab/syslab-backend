package net.diveon.backend.domain.notification.dto;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationListResponse {

    private final long total;
    private final long unreadCount;
    private final int page;
    private final int size;
    private final List<Item> notifications;

    public NotificationListResponse(long total, long unreadCount, int page, int size, List<Item> notifications) {
        this.total = total;
        this.unreadCount = unreadCount;
        this.page = page;
        this.size = size;
        this.notifications = notifications;
    }

    public long getTotal() { return total; }
    public long getUnreadCount() { return unreadCount; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public List<Item> getNotifications() { return notifications; }

    public static class Item {
        private final Long id;
        private final String type;
        private final String title;
        private final String body;
        private final String link;
        private final boolean read;
        private final LocalDateTime createdAt;

        public Item(Long id, String type, String title, String body, String link, boolean read, LocalDateTime createdAt) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.body = body;
            this.link = link;
            this.read = read;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getBody() { return body; }
        public String getLink() { return link; }
        public boolean isRead() { return read; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}
