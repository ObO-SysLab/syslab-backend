package net.diveon.backend.domain.store.dto;

// 한 사용자가 착용 중인 꾸미기. 착용하지 않은 분류는 null.
public class EquippedCosmetics {

    private final long userId;
    private String avatar;
    private String title;
    private String titleName;
    private String theme;

    public EquippedCosmetics(long userId) {
        this.userId = userId;
    }

    public void put(String category, String assetKey, String name) {
        switch (category) {
            case "avatar" -> this.avatar = assetKey;
            case "title" -> { this.title = assetKey; this.titleName = name; }
            case "theme" -> this.theme = assetKey;
            default -> { }
        }
    }

    public long getUserId() { return userId; }
    public String getAvatar() { return avatar; }
    public String getTitle() { return title; }
    public String getTitleName() { return titleName; }
    public String getTheme() { return theme; }
}
