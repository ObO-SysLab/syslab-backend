package net.diveon.backend.domain.notification.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.diveon.backend.domain.notification.dto.NotificationListResponse;
import net.diveon.backend.domain.notification.dto.NotificationSettings;
import net.diveon.backend.domain.notification.service.NotificationService;
import net.diveon.backend.global.response.ApiResponse;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> list(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("알림 조회 성공",
                notificationService.list(Long.parseLong(userId), page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("안 읽은 알림 수 조회 성공",
                Map.of("unreadCount", notificationService.unreadCount(Long.parseLong(userId)))));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@AuthenticationPrincipal String userId,
                                                      @PathVariable long notificationId) {
        notificationService.markRead(Long.parseLong(userId), notificationId);
        return ResponseEntity.ok(ApiResponse.success("알림을 읽음으로 표시했습니다.", null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead(@AuthenticationPrincipal String userId) {
        int updated = notificationService.markAllRead(Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("모든 알림을 읽음으로 표시했습니다.", Map.of("updated", updated)));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettings>> settings(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("알림 설정 조회 성공",
                notificationService.settings(Long.parseLong(userId))));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettings>> updateSettings(@AuthenticationPrincipal String userId,
                                                                           @RequestBody NotificationSettings request) {
        return ResponseEntity.ok(ApiResponse.success("알림 설정을 저장했습니다.",
                notificationService.updateSettings(Long.parseLong(userId), request)));
    }
}
