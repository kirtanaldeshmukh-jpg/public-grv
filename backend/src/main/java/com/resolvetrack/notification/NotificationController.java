package com.resolvetrack.notification;

import com.resolvetrack.auth.CustomUserDetails;
import com.resolvetrack.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = {"/api/notifications", "/api/v1/notifications"})
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationDto.Response>>> getMyNotifications(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<NotificationDto.Response> notifications = notificationService.getNotificationsForUser(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(notifications));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationDto.UnreadCountResponse>> getUnreadCount(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        long unreadCount = notificationService.getUnreadCountForUser(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(new NotificationDto.UnreadCountResponse(unreadCount)));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        notificationService.markNotificationAsRead(id, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", null));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAsReadPost(
            @PathVariable Long id,
            Authentication authentication) {
        return markAsRead(id, authentication);
    }

    @PutMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        int count = notificationService.markAllAsRead(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", count));
    }

    @PostMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Integer>> markAllAsReadPost(Authentication authentication) {
        return markAllAsRead(authentication);
    }
}

