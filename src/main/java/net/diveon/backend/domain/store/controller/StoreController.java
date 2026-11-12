package net.diveon.backend.domain.store.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.diveon.backend.domain.store.dto.EquippedCosmetics;
import net.diveon.backend.domain.store.dto.StoreItemResponse;
import net.diveon.backend.domain.store.service.StoreService;
import net.diveon.backend.global.response.ApiResponse;

@RestController
@RequestMapping("/api/store")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    // 비로그인 방문자는 "anonymousUser"로 들어온다.
    private static Long optionalUserId(String userId) {
        return userId == null || "anonymousUser".equals(userId) ? null : Long.parseLong(userId);
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<StoreItemResponse>>> items(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("상품 목록 조회 성공", storeService.items(optionalUserId(userId))));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("포인트 조회 성공", storeService.me(Long.parseLong(userId))));
    }

    @PostMapping("/items/{itemId}/purchase")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> purchase(@AuthenticationPrincipal String userId,
                                                                      @PathVariable long itemId) {
        int balance = storeService.purchase(Long.parseLong(userId), itemId);
        return ResponseEntity.ok(ApiResponse.success("구매했습니다.", Map.of("balance", balance)));
    }

    @PatchMapping("/items/{itemId}/equip")
    public ResponseEntity<ApiResponse<Void>> equip(@AuthenticationPrincipal String userId, @PathVariable long itemId) {
        storeService.equip(Long.parseLong(userId), itemId);
        return ResponseEntity.ok(ApiResponse.success("착용했습니다.", null));
    }

    @DeleteMapping("/equipped/{category}")
    public ResponseEntity<ApiResponse<Void>> unequip(@AuthenticationPrincipal String userId, @PathVariable String category) {
        storeService.unequip(Long.parseLong(userId), category);
        return ResponseEntity.ok(ApiResponse.success("착용을 해제했습니다.", null));
    }

    // 여러 사용자의 착용 상태. userIds=1,2,3 (최대 100명)
    @GetMapping("/equipped")
    public ResponseEntity<ApiResponse<List<EquippedCosmetics>>> equipped(@RequestParam(defaultValue = "") String userIds) {
        List<Long> ids = Arrays.stream(userIds.split(","))
                .map(String::trim).filter(s -> s.matches("\\d{1,18}")).map(Long::parseLong).toList();
        return ResponseEntity.ok(ApiResponse.success("착용 정보 조회 성공", storeService.equipped(ids)));
    }
}
