package net.diveon.backend.domain.store.dto;

import java.time.LocalDateTime;

// 포인트 내역 한 줄: SOLVE(문제 제목) 또는 PURCHASE(상품 이름)
public class PointHistoryItem {

    private final int amount;
    private final String reason;
    private final long refId;
    private final String label;
    private final LocalDateTime createdAt;

    public PointHistoryItem(int amount, String reason, long refId, String label, LocalDateTime createdAt) {
        this.amount = amount;
        this.reason = reason;
        this.refId = refId;
        this.label = label;
        this.createdAt = createdAt;
    }

    public int getAmount() { return amount; }
    public String getReason() { return reason; }
    public long getRefId() { return refId; }
    public String getLabel() { return label; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
