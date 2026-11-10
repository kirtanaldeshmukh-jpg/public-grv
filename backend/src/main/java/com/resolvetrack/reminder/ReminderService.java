package com.resolvetrack.reminder;

import com.resolvetrack.common.ResourceNotFoundException;
import com.resolvetrack.common.SystemSettingRepository;
import com.resolvetrack.complaint.Complaint;
import com.resolvetrack.complaint.ComplaintRepository;
import com.resolvetrack.complaint.ComplaintStatus;
import com.resolvetrack.history.ActionHistoryService;
import com.resolvetrack.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final ReminderRepository reminderRepository;
    private final ComplaintRepository complaintRepository;
    private final ActionHistoryService actionHistoryService;
    private final SystemSettingRepository systemSettingRepository;
    private final NotificationService notificationService;
    private final com.resolvetrack.auth.UserRepository userRepository;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ReminderService(
            ReminderRepository reminderRepository,
            ComplaintRepository complaintRepository,
            ActionHistoryService actionHistoryService,
            SystemSettingRepository systemSettingRepository,
            NotificationService notificationService,
            com.resolvetrack.auth.UserRepository userRepository,
            Clock clock) {
        this.reminderRepository = reminderRepository;
        this.complaintRepository = complaintRepository;
        this.actionHistoryService = actionHistoryService;
        this.systemSettingRepository = systemSettingRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public ReminderService(
            ReminderRepository reminderRepository,
            ComplaintRepository complaintRepository,
            ActionHistoryService actionHistoryService,
            SystemSettingRepository systemSettingRepository,
            NotificationService notificationService,
            Clock clock) {
        this(reminderRepository, complaintRepository, actionHistoryService, systemSettingRepository, notificationService, null, clock);
    }

    /**
     * Unified reminder processing: dispatches pre-deadline reminders and processes overdue transitions.
     * All actions are idempotent, transactional, and write action_history with actor SYSTEM.
     */
    @Transactional
    public int processReminders() {
        int nudges = dispatchPreDeadlineNudges();
        int overdue = processOverdueTransitions();
        return nudges + overdue;
    }

    /**
     * Dispatches pre-deadline reminder nudges for active complaints nearing deadline.
     * Idempotent: Does not create duplicate reminders if one is already open or recently issued.
     */
    @Transactional
    public int dispatchPreDeadlineNudges() {
        LocalDateTime now = LocalDateTime.now(clock);
        boolean demoMode = isDemoMode();
        Duration leadTime = getReminderLeadTime(demoMode);
        LocalDateTime threshold = now.plus(leadTime);

        // Find active complaints where deadline is within the lead time window and not yet passed
        List<Complaint> activeComplaints = complaintRepository.findAll().stream()
                .filter(c -> c.getStatus() != null && !c.getStatus().isResolved())
                .filter(c -> c.getAssignedOfficer() != null)
                .filter(c -> {
                    LocalDateTime deadline = c.getFollowUpDeadline() != null ? c.getFollowUpDeadline() : c.getSlaDueDate();
                    return deadline != null && deadline.isAfter(now) && !deadline.isAfter(threshold);
                })
                .toList();

        int dispatched = 0;

        for (Complaint complaint : activeComplaints) {
            // Idempotency: verify no open PRE_DEADLINE_NUDGE already exists
            boolean alreadyNudged = reminderRepository.existsByComplaintIdAndReminderTypeAndAcknowledgedFalse(
                    complaint.getId(),
                    "PRE_DEADLINE_NUDGE"
            );

            if (!alreadyNudged) {
                String trackingRef = complaint.getTrackingId() != null ? complaint.getTrackingId() : complaint.getTrackingNumber();
                LocalDateTime deadline = complaint.getFollowUpDeadline() != null ? complaint.getFollowUpDeadline() : complaint.getSlaDueDate();

                String message = String.format(
                        "SAKALA NUDGE: Complaint %s is approaching statutory SLA deadline on %s. Immediate action required.",
                        trackingRef,
                        deadline
                );

                Reminder reminder = Reminder.builder()
                        .complaint(complaint)
                        .officer(complaint.getAssignedOfficer())
                        .reminderType("PRE_DEADLINE_NUDGE")
                        .nudgeCount(1)
                        .channel("IN_APP")
                        .message(message)
                        .sentAt(now)
                        .acknowledged(false)
                        .build();

                reminderRepository.save(reminder);

                // In-app alert to officer
                if (complaint.getAssignedOfficer() != null && complaint.getAssignedOfficer().getUser() != null) {
                    notificationService.sendInAppNotification(
                            complaint.getAssignedOfficer().getUser(),
                            complaint,
                            "Pre-Deadline Reminder: " + trackingRef,
                            message
                    );
                }

                // Audit history with SYSTEM actor
                actionHistoryService.logAction(
                        complaint.getId(),
                        null,
                        "SYSTEM",
                        "SYSTEM",
                        "REMINDER_DISPATCHED",
                        complaint.getStatus().name(),
                        complaint.getStatus().name(),
                        "Automated pre-deadline nudge issued to officer " +
                                (complaint.getAssignedOfficer() != null ? complaint.getAssignedOfficer().getEmployeeCode() : "N/A"),
                        true
                );

                dispatched++;
            }
        }

        log.debug("Dispatched {} pre-deadline reminder nudges (demoMode={})", dispatched, demoMode);
        return dispatched;
    }

    /**
     * Transitions active complaints past their follow-up deadline to OVERDUE status.
     * Dispatches an OVERDUE_WARNING reminder and writes action_history with actor SYSTEM.
     */
    @Transactional
    public int processOverdueTransitions() {
        LocalDateTime now = LocalDateTime.now(clock);

        List<Complaint> overdueCandidates = complaintRepository.findAll().stream()
                .filter(c -> c.getStatus() != null
                        && !c.getStatus().isResolved()
                        && c.getStatus() != ComplaintStatus.OVERDUE
                        && c.getStatus() != ComplaintStatus.ESCALATED
                        && c.getStatus() != ComplaintStatus.RESOLUTION_SUBMITTED)
                .filter(c -> {
                    LocalDateTime deadline = c.getFollowUpDeadline() != null ? c.getFollowUpDeadline() : c.getSlaDueDate();
                    return deadline != null && (now.isAfter(deadline) || now.isEqual(deadline));
                })
                .toList();

        int transitioned = 0;

        for (Complaint complaint : overdueCandidates) {
            ComplaintStatus prevStatus = complaint.getStatus();
            complaint.setStatus(ComplaintStatus.OVERDUE);
            complaintRepository.save(complaint);

            String trackingRef = complaint.getTrackingId() != null ? complaint.getTrackingId() : complaint.getTrackingNumber();

            // Send urgent in-app notification to assigned officer
            if (complaint.getAssignedOfficer() != null && complaint.getAssignedOfficer().getUser() != null) {
                String message = String.format(
                        "SAKALA BREACH WARNING: Complaint %s has expired statutory follow-up deadline. Transitioned to OVERDUE.",
                        trackingRef
                );
                notificationService.sendInAppNotification(
                        complaint.getAssignedOfficer().getUser(),
                        complaint,
                        "Urgent: Grievance Overdue " + trackingRef,
                        message
                );
            }

            // Write action history with SYSTEM actor
            actionHistoryService.logAction(
                    complaint.getId(),
                    null,
                    "SYSTEM",
                    "SYSTEM",
                    "OVERDUE_TRANSITION",
                    prevStatus.name(),
                    ComplaintStatus.OVERDUE.name(),
                    "Complaint passed follow-up deadline. Transitioned to OVERDUE by follow-up engine.",
                    true
            );

            transitioned++;
        }

        log.debug("Processed {} overdue transitions", transitioned);
        return transitioned;
    }

    @Transactional(readOnly = true)
    public List<ReminderDto.Response> getAllReminders() {
        return reminderRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "sentAt")).stream()
                .map(r -> ReminderDto.Response.builder()
                        .id(r.getId())
                        .complaintId(r.getComplaint().getId())
                        .complaintTrackingNumber(r.getComplaint().getTrackingId() != null ? r.getComplaint().getTrackingId() : r.getComplaint().getTrackingNumber())
                        .officerId(r.getOfficer().getId())
                        .reminderType(r.getReminderType())
                        .nudgeCount(r.getNudgeCount())
                        .channel(r.getChannel())
                        .message(r.getMessage())
                        .sentAt(r.getSentAt())
                        .acknowledged(r.isAcknowledged())
                        .acknowledgedAt(r.getAcknowledgedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReminderDto.Response> getOfficerReminders(Long officerId) {
        return reminderRepository.findAllByOfficerIdOrderBySentAtDesc(officerId).stream()
                .map(r -> ReminderDto.Response.builder()
                        .id(r.getId())
                        .complaintId(r.getComplaint().getId())
                        .complaintTrackingNumber(r.getComplaint().getTrackingId() != null ? r.getComplaint().getTrackingId() : r.getComplaint().getTrackingNumber())
                        .officerId(r.getOfficer().getId())
                        .reminderType(r.getReminderType())
                        .nudgeCount(r.getNudgeCount())
                        .channel(r.getChannel())
                        .message(r.getMessage())
                        .sentAt(r.getSentAt())
                        .acknowledged(r.isAcknowledged())
                        .acknowledgedAt(r.getAcknowledgedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void acknowledgeReminder(Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found with id: " + reminderId));
        reminder.setAcknowledged(true);
        reminder.setAcknowledgedAt(LocalDateTime.now(clock));
        reminderRepository.save(reminder);
    }

    @Transactional
    public int acknowledgeAllOpenRemindersForComplaint(Long complaintId) {
        List<Reminder> unacknowledged = reminderRepository.findAllByComplaintIdAndAcknowledgedFalse(complaintId);
        LocalDateTime now = LocalDateTime.now(clock);
        for (Reminder r : unacknowledged) {
            r.setAcknowledged(true);
            r.setAcknowledgedAt(now);
            reminderRepository.save(r);
        }
        return unacknowledged.size();
    }

    private boolean isDemoMode() {
        return systemSettingRepository.findBySettingKey("demo.mode")
                .map(s -> "true".equalsIgnoreCase(s.getSettingValue().trim()))
                .orElse(false);
    }

    private Duration getReminderLeadTime(boolean demoMode) {
        int val = systemSettingRepository.findBySettingKey("reminder.lead_time")
                .or(() -> systemSettingRepository.findBySettingKey("sla.warning.hours"))
                .map(s -> {
                    try { return Integer.parseInt(s.getSettingValue().trim()); } catch (NumberFormatException e) { return 24; }
                })
                .orElse(24);
        return demoMode ? Duration.ofMinutes(val) : Duration.ofHours(val);
    }

    /**
     * Checks complaints in RESOLUTION_SUBMITTED past resolution.citizen.review.timeout.days.
     * Flags for supervisor review instead of auto-resolving (Prompt 7.3).
     */
    @Transactional
    public int processCitizenReviewTimeouts() {
        LocalDateTime now = LocalDateTime.now(clock);
        boolean isDemo = isDemoMode();
        long timeoutDays = getCitizenReviewTimeout();

        LocalDateTime cutoff = isDemo ? now.minusMinutes(timeoutDays) : now.minusDays(timeoutDays);

        List<Complaint> candidates = complaintRepository.findAll().stream()
                .filter(c -> c.getStatus() == ComplaintStatus.RESOLUTION_SUBMITTED
                        && c.getResolutionSubmittedAt() != null
                        && c.getResolutionSubmittedAt().isBefore(cutoff))
                .toList();

        int flagged = 0;
        for (Complaint complaint : candidates) {
            boolean alreadyFlagged = actionHistoryService.getInternalHistory(complaint.getId()).stream()
                    .anyMatch(h -> "REVIEW_TIMEOUT_SUPERVISOR_FLAGGED".equals(h.getAction()));

            if (!alreadyFlagged) {
                actionHistoryService.logAction(
                        complaint.getId(),
                        null,
                        "SYSTEM",
                        "SYSTEM",
                        "REVIEW_TIMEOUT_SUPERVISOR_FLAGGED",
                        ComplaintStatus.RESOLUTION_SUBMITTED.name(),
                        ComplaintStatus.RESOLUTION_SUBMITTED.name(),
                        "Citizen verification window expired without confirmation. Escalated for supervisor sign-off.",
                        true
                );

                com.resolvetrack.auth.User supervisor = findSupervisorForDepartment(
                        complaint.getDepartment() != null ? complaint.getDepartment().getId() : null);

                if (supervisor != null && notificationService != null) {
                    notificationService.sendInAppNotification(
                            supervisor,
                            complaint,
                            "Citizen Review Timeout: " + complaint.getTrackingId(),
                            "Citizen has not verified resolution within the statutory review period. Supervisor sign-off required for docket closure."
                    );
                }

                flagged++;
            }
        }
        return flagged;
    }

    private long getCitizenReviewTimeout() {
        return systemSettingRepository.findBySettingKey("resolution.citizen.review.timeout.days")
                .map(s -> {
                    try {
                        return Long.parseLong(s.getSettingValue().trim());
                    } catch (NumberFormatException e) {
                        return 3L;
                    }
                })
                .orElse(3L);
    }

    private com.resolvetrack.auth.User findSupervisorForDepartment(Long departmentId) {
        if (userRepository == null) return null;
        if (departmentId != null) {
            Optional<com.resolvetrack.auth.User> sup = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == com.resolvetrack.auth.Role.SUPERVISOR && departmentId.equals(u.getDepartmentId()))
                    .findFirst();
            if (sup.isPresent()) return sup.get();
        }
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == com.resolvetrack.auth.Role.ADMIN)
                .findFirst()
                .orElse(null);
    }
}
