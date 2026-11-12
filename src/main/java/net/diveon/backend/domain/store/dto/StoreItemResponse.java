package net.diveon.backend.domain.store.dto;

// 상점 상품. owned/equipped는 로그인했을 때만 의미가 있다(아니면 false).
public class StoreItemResponse {

    private final long itemId;
    private final String category;
    private final String name;
    private final String description;
    private final int price;
    private final String rarity;
    private final String assetKey;
    private final boolean owned;
    private final boolean equipped;

    public StoreItemResponse(long itemId, String category, String name, String description, int price,
                             String rarity, String assetKey, boolean owned, boolean equipped) {
        this.itemId = itemId;
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.rarity = rarity;
        this.assetKey = assetKey;
        this.owned = owned;
        this.equipped = equipped;
    }

    public long getItemId() { return itemId; }
    public String getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPrice() { return price; }
    public String getRarity() { return rarity; }
    public String getAssetKey() { return assetKey; }
    public boolean isOwned() { return owned; }
    public boolean isEquipped() { return equipped; }
}
