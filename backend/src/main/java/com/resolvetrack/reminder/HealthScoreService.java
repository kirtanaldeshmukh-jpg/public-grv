package com.resolvetrack.reminder;

import com.resolvetrack.complaint.Complaint;
import com.resolvetrack.complaint.ComplaintRepository;
import com.resolvetrack.complaint.ComplaintStatus;
import com.resolvetrack.history.ActionHistory;
import com.resolvetrack.history.ActionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class HealthScoreService {

    private static final Logger log = LoggerFactory.getLogger(HealthScoreService.class);

    private final ComplaintRepository complaintRepository;
    private final ReminderRepository reminderRepository;
    private final ActionHistoryRepository actionHistoryRepository;
    private final Clock clock;

    public HealthScoreService(
            ComplaintRepository complaintRepository,
            ReminderRepository reminderRepository,
            ActionHistoryRepository actionHistoryRepository,
            Clock clock) {
        this.complaintRepository = complaintRepository;
        this.reminderRepository = reminderRepository;
        this.actionHistoryRepository = actionHistoryRepository;
        this.clock = clock;
    }

    /**
     * Calculates the deterministic health score (0 - 100), status band, and human-readable reasons with i18n keys.
     * 100 = Healthy, on-track; < 50 = Critical risk of SLA breach.
     */
    public HealthScoreResult calculateHealthDetail(Complaint complaint, LocalDateTime now) {
        if (complaint == null) {
            return new HealthScoreResult(100, HealthBand.HEALTHY, List.of("No complaint record"),
                    List.of(new HealthScoreResult.ReasonItem("health.reason.no_record", Map.of(), "No complaint record")));
        }

        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            return new HealthScoreResult(100, HealthBand.HEALTHY, List.of("Grievance resolved and verified"),
                    List.of(new HealthScoreResult.ReasonItem("health.reason.resolved", Map.of(), "Grievance resolved and verified")));
        }
        if (complaint.getStatus() == ComplaintStatus.REJECTED) {
            return new HealthScoreResult(0, HealthBand.CRITICAL, List.of("Grievance dismissed or rejected"),
                    List.of(new HealthScoreResult.ReasonItem("health.reason.rejected", Map.of(), "Grievance dismissed or rejected")));
        }

        int score = 100;
        List<String> reasons = new ArrayList<>();
        List<HealthScoreResult.ReasonItem> reasonItems = new ArrayList<>();

        // 1. Escalation Penalties
        int escalationLevel = complaint.getEscalationLevel() != null ? complaint.getEscalationLevel() : 0;
        if (escalationLevel == 1) {
            score -= 25;
            String msg = "Level 1 statutory escalation active (-25 pts)";
            reasons.add(msg);
            reasonItems.add(new HealthScoreResult.ReasonItem("health.reason.escalation_level_1", Map.of("level", 1, "points", 25), msg));
        } else if (escalationLevel == 2) {
            score -= 50;
            String msg = "Level 2 statutory escalation active (-50 pts)";
            reasons.add(msg);
            reasonItems.add(new HealthScoreResult.ReasonItem("health.reason.escalation_level_2", Map.of("level", 2, "points", 50), msg));
        } else if (escalationLevel >= 3) {
            score -= 75;
            String msg = "Level 3 statutory escalation active (-75 pts)";
            reasons.add(msg);
            reasonItems.add(new HealthScoreResult.ReasonItem("health.reason.escalation_level_3", Map.of("level", 3, "points", 75), msg));
        }

        // 2. Overdue & Proximity Penalties
        LocalDateTime deadline = complaint.getFollowUpDeadline() != null
                ? complaint.getFollowUpDeadline()
                : complaint.getSlaDueDate();

        if (deadline != null) {
            if (now.isAfter(deadline)) {
                score -= 30; // Direct SLA breach penalty
                String msg = "SLA deadline breached / overdue (-30 pts)";
                reasons.add(msg);
                reasonItems.add(new HealthScoreResult.ReasonItem("health.reason.deadline_breached", Map.of("points", 30), msg));
            } else {
                Duration remaining = Duration.between(now, deadline);
                if (!remaining.isNegative() && remaining.toHours() <= 24) {
                    score -= 10; // Approaching deadline penalty
                    String msg = "SLA deadline expiring within 24 hours (-10 pts)";
                    reasons.add(msg);
                    reasonItems.add(new HealthScoreResult.ReasonItem("health.reason.deadline_expiring_24h", Map.of("points", 10), msg));
                }
            }
        }

        // 3. Ignored Reminders Penalty (-10 per unacknowledged reminder, max -30)
        if (complaint.getId() != null && reminderRepository != null) {
            long unackedReminders = reminderRepository.countByComplaintIdAndAcknowledgedFalse(complaint.getId());
            if (unackedReminders > 0) {
                int reminderPenalty = (int) Math.min(unackedReminders * 10, 30);
                score -= reminderPenalty;
                String msg = unackedReminders + " unacknowledged reminder(s) pending response (-" + reminderPenalty + " pts)";
                reasons.add(msg);
                reasonItems.add(new HealthScoreResult.ReasonItem("health.reason.unacknowledged_reminders",
                        Map.of("count", unackedReminders, "points", reminderPenalty), msg));
            }
        }

        // 4. Reopen penalty (-20 per reopen, max -40)
        int reopenCount = complaint.getReopenCount() != null ? complaint.getReopenCount() : 0;
        if (reopenCount > 0) {
            int reopenPenalty = Math.min(reopenCount * 20, 40);
            score -= reopenPenalty;
            String msg = "Citizen dispute: Reopened " + reopenCount + " time(s) (-" + reopenPenalty + " pts)";
            reasons.add(msg);
            reasonItems.add(new HealthScoreResult.ReasonItem("health.reason.reopened_dispute",
                    Map.of("count", reopenCount, "points", reopenPenalty), msg));
        }

        // 5. Recent Activity Bonus (+10 if progress made within last 24h)
        if (complaint.getId() != null && actionHistoryRepository != null) {
            List<ActionHistory> histories = actionHistoryRepository
                    .findAllByComplaintIdOrderByCreatedAtAsc(complaint.getId());
            if (!histories.isEmpty()) {
                ActionHistory latest = histories.get(histories.size() - 1);
                if (latest.getCreatedAt() != null && latest.getCreatedAt().isAfter(now.minusHours(24))) {
                    score += 10;
                    String msg = "Active investigation progress logged within last 24 hours (+10 pts bonus)";
                    reasons.add(msg);
                    reasonItems.add(new HealthScoreResult.ReasonItem("health.reason.recent_progress_bonus", Map.of("points", 10), msg));
                }
            }
        }

        int finalScore = Math.max(0, Math.min(100, score));
        HealthBand band = HealthBand.fromScore(finalScore);

        if (reasons.isEmpty()) {
            String msg = "Grievance progressing on schedule with healthy SLA compliance";
            reasons.add(msg);
            reasonItems.add(new HealthScoreResult.ReasonItem("health.reason.progressing_healthy", Map.of(), msg));
        }

        return new HealthScoreResult(finalScore, band, reasons, reasonItems);
    }

    /**
     * Calculates the deterministic health score (0 - 100) of a complaint.
     */
    public int calculateHealthScore(Complaint complaint, LocalDateTime now) {
        return calculateHealthDetail(complaint, now).getScore();
    }

    @Transactional
    public void updateComplaintHealthScore(Complaint complaint, LocalDateTime now) {
        int newScore = calculateHealthScore(complaint, now);
        complaint.setHealthScore(newScore);
        complaintRepository.save(complaint);
    }

    @Transactional
    public int recalculateAll() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Complaint> activeComplaints = complaintRepository.findAll().stream()
                .filter(c -> c.getStatus() != null && !c.getStatus().isResolved())
                .toList();

        for (Complaint complaint : activeComplaints) {
            updateComplaintHealthScore(complaint, now);
        }

        log.debug("Recalculated health scores for {} active complaints", activeComplaints.size());
        return activeComplaints.size();
    }
}
