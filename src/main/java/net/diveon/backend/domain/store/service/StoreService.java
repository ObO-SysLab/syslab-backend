package net.diveon.backend.domain.store.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.store.dto.EquippedCosmetics;
import net.diveon.backend.domain.store.dto.PointHistoryItem;
import net.diveon.backend.domain.store.dto.StoreItemResponse;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.DomainRuleException;
import net.diveon.backend.global.exception.UserNotFoundException;

// 상점: 상품 목록, 구매, 착용. 테이블은 ops/2026-11-12-account-store-notification.sql.
@Service
public class StoreService {

    static final Set<String> CATEGORIES = Set.of("avatar", "title", "theme");
    static final int MAX_LOOKUP = 100;

    static final String ITEMS_SQL = """
            SELECT si.id, si.category, si.name, si.description, si.price, si.rarity, si.asset_key,
                   (ui.id IS NOT NULL) AS owned, COALESCE(ui.equipped, false) AS equipped
            FROM store_item si
            LEFT JOIN user_item ui ON ui.item_id = si.id AND ui.user_id = ?
            WHERE si.is_active = true
            ORDER BY si.sort_order, si.id
            """;

    static final String EQUIPPED_SQL_PREFIX = """
            SELECT ui.user_id, si.category, si.asset_key, si.name
            FROM user_item ui
            JOIN store_item si ON si.id = ui.item_id
            JOIN domain_user u ON u.id = ui.user_id AND u.deleted_at IS NULL
            WHERE ui.equipped = true AND ui.user_id IN (""";

    private final JdbcTemplate jdbc;
    private final PointService points;
    private final UserRepository users;

    public StoreService(JdbcTemplate jdbc, PointService points, UserRepository users) {
        this.jdbc = jdbc;
        this.points = points;
        this.users = users;
    }

    protected LocalDateTime now() {
        return LocalDateTime.now();
    }

    // userId가 null이면 비로그인(보유·착용 표시 없음)
    @Transactional(readOnly = true)
    public List<StoreItemResponse> items(Long userId) {
        return jdbc.query(ITEMS_SQL, (rs, i) -> new StoreItemResponse(
                rs.getLong("id"), rs.getString("category"), rs.getString("name"), rs.getString("description"),
                rs.getInt("price"), rs.getString("rarity"), rs.getString("asset_key"),
                rs.getBoolean("owned"), rs.getBoolean("equipped")), userId == null ? -1L : userId);
    }

    // 잔액·획득·사용·최근 내역. 조회 전에 풀이 기록을 장부로 정산한다.
    @Transactional
    public Map<String, Object> me(long userId) {
        points.reconcile(userId);
        Map<String, Object> sums = jdbc.queryForMap("""
                SELECT COALESCE(SUM(amount), 0) AS balance,
                       COALESCE(SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END), 0) AS earned,
                       COALESCE(SUM(CASE WHEN amount < 0 THEN -amount ELSE 0 END), 0) AS spent
                FROM point_ledger WHERE user_id = ?
                """, userId);
        List<PointHistoryItem> history = points.history(userId, 20);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("balance", ((Number) sums.get("balance")).intValue());
        result.put("earned", ((Number) sums.get("earned")).intValue());
        result.put("spent", ((Number) sums.get("spent")).intValue());
        result.put("history", history);
        return result;
    }

    // 구매: 사용자 행을 잠가 동시에 두 번 사도 잔액이 음수가 되지 않게 한다.
    @Transactional
    public int purchase(long userId, long itemId) {
        users.lockVmOwner(userId).orElseThrow(UserNotFoundException::new);
        List<Integer> price = jdbc.query("SELECT price FROM store_item WHERE id = ? AND is_active = true",
                (rs, i) -> rs.getInt(1), itemId);
        if (price.isEmpty()) throw new DomainRuleException(HttpStatus.NOT_FOUND, "판매 중인 상품이 아닙니다.");
        if (owns(userId, itemId)) throw new DomainRuleException(HttpStatus.CONFLICT, "이미 보유한 상품입니다.");

        points.reconcile(userId);
        int balance = points.balance(userId);
        int cost = price.get(0);
        if (balance < cost) {
            throw new DomainRuleException(HttpStatus.BAD_REQUEST, "포인트가 부족합니다. (보유 " + balance + "P, 필요 " + cost + "P)");
        }
        jdbc.update("INSERT INTO user_item (user_id, item_id, equipped, purchased_at) VALUES (?, ?, false, ?)",
                userId, itemId, Timestamp.valueOf(now()));
        points.spend(userId, itemId, cost);
        return balance - cost;
    }

    // 착용: 같은 분류에서는 하나만 착용된다.
    @Transactional
    public void equip(long userId, long itemId) {
        List<String> category = jdbc.query("""
                SELECT si.category FROM user_item ui JOIN store_item si ON si.id = ui.item_id
                WHERE ui.user_id = ? AND ui.item_id = ?
                """, (rs, i) -> rs.getString(1), userId, itemId);
        if (category.isEmpty()) throw new DomainRuleException(HttpStatus.NOT_FOUND, "보유하지 않은 상품입니다.");
        unequip(userId, category.get(0));
        jdbc.update("UPDATE user_item SET equipped = true WHERE user_id = ? AND item_id = ?", userId, itemId);
    }

    @Transactional
    public void unequip(long userId, String category) {
        if (!CATEGORIES.contains(category)) throw new DomainRuleException(HttpStatus.BAD_REQUEST, "알 수 없는 분류입니다.");
        jdbc.update("""
                UPDATE user_item SET equipped = false
                WHERE user_id = ? AND equipped = true
                  AND item_id IN (SELECT id FROM store_item WHERE category = ?)
                """, userId, category);
    }

    // 여러 사용자의 착용 상태(랭킹·댓글·멤버 표 등에서 한 번에 조회). 탈퇴자는 빠진다.
    @Transactional(readOnly = true)
    public List<EquippedCosmetics> equipped(List<Long> userIds) {
        List<Long> ids = userIds.stream().distinct().limit(MAX_LOOKUP).toList();
        if (ids.isEmpty()) return List.of();
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        Map<Long, EquippedCosmetics> byUser = new LinkedHashMap<>();
        jdbc.query(EQUIPPED_SQL_PREFIX + placeholders + ")", rs -> {
            long uid = rs.getLong("user_id");
            byUser.computeIfAbsent(uid, EquippedCosmetics::new)
                    .put(rs.getString("category"), rs.getString("asset_key"), rs.getString("name"));
        }, ids.toArray());
        return new ArrayList<>(byUser.values());
    }

    // 계정 탈퇴: 착용을 모두 해제한다(탈퇴 트랜잭션 안에서 호출).
    public void unequipAll(long userId) {
        jdbc.update("UPDATE user_item SET equipped = false WHERE user_id = ?", userId);
    }

    private boolean owns(long userId, long itemId) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM user_item WHERE user_id = ? AND item_id = ?",
                Integer.class, userId, itemId);
        return n != null && n > 0;
    }
}
