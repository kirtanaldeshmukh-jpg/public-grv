package com.resolvetrack.notification;

import com.resolvetrack.auth.User;
import com.resolvetrack.common.ResourceNotFoundException;
import com.resolvetrack.complaint.Complaint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public NotificationService() {
        this(null, Clock.systemDefaultZone());
    }

    public NotificationService(NotificationRepository notificationRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    @Transactional
    public Notification sendInAppNotification(User recipient, Complaint complaint, String title, String message) {
        if (recipient == null || notificationRepository == null) {
            log.warn("Cannot create in-app notification without recipient user or repository");
            return null;
        }

        Notification notification = Notification.builder()
                .user(recipient)
                .complaint(complaint)
                .title(title)
                .message(message)
                .channel("IN_APP")
                .read(false)
                .createdAt(LocalDateTime.now(clock))
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("[IN-APP NOTIFICATION] Saved alert #{} for user {}: {}", saved.getId(), recipient.getUsername(), title);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto.Response> getNotificationsForUser(Long userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCountForUser(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markNotificationAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now(clock));
        notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsReadForUser(userId, LocalDateTime.now(clock));
    }

    public void sendCitizenNotification(String phoneNumber, String message) {
        log.info("[SMS GATEWAY] Dispatching to {}: {}", phoneNumber, message);
    }

    public void sendOfficerNudge(String email, String subject, String content) {
        log.info("[OFFICER DISPATCH] Email to {}: Subject: {} | Content: {}", email, subject, content);
    }

    public void dispatch(NotificationDto.Message message) {
        log.info("[NOTIFICATION ENGINE] Channel: {} | To: {} | Subject: {}",
                message.getChannel(), message.getRecipient(), message.getSubject());
    }

    private NotificationDto.Response mapToResponse(Notification n) {
        String trackingId = n.getComplaint() != null ? n.getComplaint().getTrackingId() : null;
        Long complaintId = n.getComplaint() != null ? n.getComplaint().getId() : null;
        return new NotificationDto.Response(
                n.getId(),
                n.getUser().getId(),
                complaintId,
                trackingId,
                n.getTitle(),
                n.getMessage(),
                n.getChannel(),
                n.isRead(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}
