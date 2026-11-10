package com.resolvetrack.escalation;

import com.resolvetrack.auth.Role;
import com.resolvetrack.auth.User;
import com.resolvetrack.auth.UserRepository;
import com.resolvetrack.common.ResourceNotFoundException;
import com.resolvetrack.common.SystemSettingRepository;
import com.resolvetrack.complaint.Complaint;
import com.resolvetrack.complaint.ComplaintRepository;
import com.resolvetrack.complaint.ComplaintStatus;
import com.resolvetrack.history.ActionHistoryService;
import com.resolvetrack.notification.NotificationService;
import com.resolvetrack.reminder.ReminderRepository;
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
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

    private final EscalationRepository escalationRepository;
    private final ComplaintRepository complaintRepository;
    private final ReminderRepository reminderRepository;
    private final ActionHistoryService actionHistoryService;
    private final SystemSettingRepository systemSettingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    public EscalationService(
            EscalationRepository escalationRepository,
            ComplaintRepository complaintRepository,
            ReminderRepository reminderRepository,
            ActionHistoryService actionHistoryService,
            SystemSettingRepository systemSettingRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            Clock clock) {
        this.escalationRepository = escalationRepository;
        this.complaintRepository = complaintRepository;
        this.reminderRepository = reminderRepository;
        this.actionHistoryService = actionHistoryService;
        this.systemSettingRepository = systemSettingRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * Unified statutory escalation processor.
     * Evaluates delay breaches, ignored reminder thresholds, and multi-tier statutory escalations.
     * All operations are idempotent, transactional, and audit with SYSTEM actor.
     */
    @Transactional
    public int processEscalations() {
        return processStatutoryEscalations();
    }

    @Transactional
    public int processStatutoryEscalations() {
        LocalDateTime now = LocalDateTime.now(clock);
        boolean demoMode = isDemoMode();
        Duration tier1Delay = getEscalationDelay(demoMode);
        Duration tier2Delay = getEscalationLevel2Delay(demoMode);
        int ignoredThreshold = getReminderIgnoredThreshold();

        List<Complaint> activeComplaints = complaintRepository.findAll().stream()
                .filter(c -> c.getStatus() != null && !c.getStatus().isResolved())
                .toList();

        int escalatedCount = 0;

        for (Complaint complaint : activeComplaints) {
            LocalDateTime deadline = complaint.getFollowUpDeadline() != null ? complaint.getFollowUpDeadline() : complaint.getSlaDueDate();
            if (deadline == null) continue;

            int currentLevel = complaint.getEscalationLevel() != null ? complaint.getEscalationLevel() : 0;

            // -----------------------------------------------------------------
            // TIER 1 ESCALATION (Level 0 -> Level 1)
            // -----------------------------------------------------------------
            if (currentLevel == 0) {
                boolean overduePastTier1 = now.isAfter(deadline.plus(tier1Delay)) || now.isEqual(deadline.plus(tier1Delay));
                long unackedReminders = reminderRepository.countByComplaintIdAndAcknowledgedFalse(complaint.getId());
                boolean overdueWithIgnoredReminders = now.isAfter(deadline) && unackedReminders >= ignoredThreshold;

                if (overduePastTier1 || overdueWithIgnoredReminders) {
                    String reason = overdueWithIgnoredReminders
                            ? String.format("SAKALA Statutory Breach: Level-1 escalation triggered due to %d ignored reminder nudges past deadline.", unackedReminders)
                            : String.format("SAKALA Statutory Breach: SLA expired on %s. Overdue past Tier-1 delay window of %d %s.",
                                    deadline,
                                    demoMode ? tier1Delay.toMinutes() : tier1Delay.toHours(),
                                    demoMode ? "minutes" : "hours");

                    escalateComplaint(complaint, 0, 1, reason, now);
                    escalatedCount++;
                }
            }
            // -----------------------------------------------------------------
            // TIER 2 ESCALATION (Level 1 -> Level 2)
            // -----------------------------------------------------------------
            else if (currentLevel == 1) {
                // Check if overdue past tier 2 threshold
                boolean overduePastTier2 = now.isAfter(deadline.plus(tier2Delay)) || now.isEqual(deadline.plus(tier2Delay));

                // Or check time since level-1 escalation occurred
                List<Escalation> pastEscalations = escalationRepository.findAllByComplaintIdOrderByEscalatedAtDesc(complaint.getId());
                boolean tier1IgnoredLongEnough = false;
                if (!pastEscalations.isEmpty()) {
                    Escalation l1 = pastEscalations.get(0);
                    if (l1.getToLevel() == 1 && l1.getEscalatedAt() != null) {
                        tier1IgnoredLongEnough = now.isAfter(l1.getEscalatedAt().plus(tier1Delay));
                    }
                }

                if (overduePastTier2 || tier1IgnoredLongEnough) {
                    String reason = String.format(
                            "SAKALA Statutory Breach: Level-2 escalation to Appellate Authority / District Admin. Overdue past Tier-2 threshold of %d %s.",
                            demoMode ? tier2Delay.toMinutes() : tier2Delay.toHours(),
                            demoMode ? "minutes" : "hours"
                    );

                    escalateComplaint(complaint, 1, 2, reason, now);
                    escalatedCount++;
                }
            }
        }

        log.debug("Processed statutory escalations. Escalated: {} complaints (demoMode={})", escalatedCount, demoMode);
        return escalatedCount;
    }

    private void escalateComplaint(Complaint complaint, int fromLevel, int toLevel, String reason, LocalDateTime now) {
        ComplaintStatus prevStatus = complaint.getStatus();
        complaint.setEscalationLevel(toLevel);
        complaint.setStatus(ComplaintStatus.ESCALATED);
        complaintRepository.save(complaint);

        // Find responsible authority to notify
        User notifiedUser = null;
        if (toLevel == 1) {
            notifiedUser = findSupervisorForDepartment(complaint.getDepartment() != null ? complaint.getDepartment().getId() : null);
        } else {
            notifiedUser = findAdminUser();
        }

        Long notifiedId = notifiedUser != null ? notifiedUser.getId() : null;

        Escalation escalation = Escalation.builder()
                .complaint(complaint)
                .fromLevel(fromLevel)
                .toLevel(toLevel)
                .triggerReason(reason)
                .notifiedSupervisorId(notifiedId)
                .acknowledged(false)
                .escalatedAt(now)
                .build();

        escalationRepository.save(escalation);

        String trackingRef = complaint.getTrackingId() != null ? complaint.getTrackingId() : complaint.getTrackingNumber();

        // Dispatch alert notification to authority
        if (notifiedUser != null) {
            notificationService.sendInAppNotification(
                    notifiedUser,
                    complaint,
                    String.format("STATUTORY ESCALATION [Level %d]: %s", toLevel, trackingRef),
                    reason
            );
        }

        // Immutable action history entry with SYSTEM actor
        actionHistoryService.logAction(
                complaint.getId(),
                null,
                "SYSTEM",
                "SYSTEM",
                "ESCALATED",
                prevStatus != null ? prevStatus.name() : "OVERDUE",
                ComplaintStatus.ESCALATED.name(),
                reason,
                true
        );
    }

    private User findSupervisorForDepartment(Long departmentId) {
        if (departmentId == null) return findAdminUser();
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPERVISOR && departmentId.equals(u.getDepartmentId()))
                .findFirst()
                .orElseGet(this::findAdminUser);
    }

    private User findAdminUser() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<EscalationDto.Response> getEscalationHistory(Long complaintId) {
        return escalationRepository.findAllByComplaintIdOrderByEscalatedAtDesc(complaintId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EscalationDto.Response> getSupervisorEscalations(Long supervisorId) {
        return escalationRepository.findAllByNotifiedSupervisorIdOrderByEscalatedAtDesc(supervisorId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void acknowledgeEscalation(Long escalationId) {
        Escalation escalation = escalationRepository.findById(escalationId)
                .orElseThrow(() -> new ResourceNotFoundException("Escalation not found with id: " + escalationId));
        escalation.setAcknowledged(true);
        escalationRepository.save(escalation);
    }

    @Transactional
    public int acknowledgeAllOpenEscalationsForComplaint(Long complaintId) {
        List<Escalation> unacked = escalationRepository.findAllByComplaintIdAndAcknowledgedFalse(complaintId);
        for (Escalation e : unacked) {
            e.setAcknowledged(true);
            escalationRepository.save(e);
        }
        return unacked.size();
    }

    @Transactional(readOnly = true)
    public List<EscalationDto.Response> getAllEscalations() {
        return escalationRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "escalatedAt")).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private EscalationDto.Response mapToDto(Escalation e) {
        String trackingRef = e.getComplaint() != null
                ? (e.getComplaint().getTrackingId() != null ? e.getComplaint().getTrackingId() : e.getComplaint().getTrackingNumber())
                : null;
        return EscalationDto.Response.builder()
                .id(e.getId())
                .complaintId(e.getComplaint() != null ? e.getComplaint().getId() : null)
                .complaintTrackingNumber(trackingRef)
                .fromLevel(e.getFromLevel())
                .toLevel(e.getToLevel())
                .triggerReason(e.getTriggerReason())
                .notifiedSupervisorId(e.getNotifiedSupervisorId())
                .acknowledged(e.isAcknowledged())
                .escalatedAt(e.getEscalatedAt())
                .build();
    }

    private boolean isDemoMode() {
        return systemSettingRepository.findBySettingKey("demo.mode")
                .map(s -> "true".equalsIgnoreCase(s.getSettingValue().trim()))
                .orElse(false);
    }

    private Duration getEscalationDelay(boolean demoMode) {
        int val = systemSettingRepository.findBySettingKey("escalation.delay")
                .or(() -> systemSettingRepository.findBySettingKey("escalation.tier1.hours"))
                .map(s -> {
                    try { return Integer.parseInt(s.getSettingValue().trim()); } catch (NumberFormatException e) { return 48; }
                })
                .orElse(48);
        return demoMode ? Duration.ofMinutes(val) : Duration.ofHours(val);
    }

    private Duration getEscalationLevel2Delay(boolean demoMode) {
        int val = systemSettingRepository.findBySettingKey("escalation.level2.delay")
                .or(() -> systemSettingRepository.findBySettingKey("escalation.tier2.hours"))
                .map(s -> {
                    try { return Integer.parseInt(s.getSettingValue().trim()); } catch (NumberFormatException e) { return 96; }
                })
                .orElse(96);
        return demoMode ? Duration.ofMinutes(val) : Duration.ofHours(val);
    }

    private int getReminderIgnoredThreshold() {
        return systemSettingRepository.findBySettingKey("reminder.ignored.threshold")
                .map(s -> {
                    try { return Integer.parseInt(s.getSettingValue().trim()); } catch (NumberFormatException e) { return 2; }
                })
                .orElse(2);
    }
}
