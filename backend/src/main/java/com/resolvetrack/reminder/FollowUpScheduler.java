package com.resolvetrack.reminder;

import com.resolvetrack.common.SystemSettingRepository;
import com.resolvetrack.escalation.EscalationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class FollowUpScheduler {

    private static final Logger log = LoggerFactory.getLogger(FollowUpScheduler.class);

    private final ReminderService reminderService;
    private final EscalationService escalationService;
    private final HealthScoreService healthScoreService;
    private final SystemSettingRepository systemSettingRepository;
    private final Clock clock;

    public FollowUpScheduler(
            ReminderService reminderService,
            EscalationService escalationService,
            HealthScoreService healthScoreService,
            SystemSettingRepository systemSettingRepository,
            Clock clock) {
        this.reminderService = reminderService;
        this.escalationService = escalationService;
        this.healthScoreService = healthScoreService;
        this.systemSettingRepository = systemSettingRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "${resolvetrack.scheduling.followup-cron:0 */5 * * * *}")
    public void scheduledRun() {
        log.info("Starting scheduled follow-up cycle execution...");
        CycleResult result = runCycle();
        log.info("Follow-up cycle completed: {}", result);
    }

    /**
     * Executes a complete, idempotent follow-up cycle.
     * Can be invoked by @Scheduled or manually via POST /api/dev/demo/run-cycle.
     */
    @Transactional
    public CycleResult runCycle() {
        LocalDateTime now = LocalDateTime.now(clock);
        boolean demoMode = systemSettingRepository.findBySettingKey("demo.mode")
                .map(s -> "true".equalsIgnoreCase(s.getSettingValue().trim()))
                .orElse(false);

        // 1. Dispatch pre-deadline reminders
        int reminders = reminderService.dispatchPreDeadlineNudges();

        // 2. Mark overdue complaints
        int overdue = reminderService.processOverdueTransitions();

        // 3. Process statutory escalations
        int escalations = escalationService.processStatutoryEscalations();

        // 4. Process citizen review timeouts (flag for supervisor review)
        int reviewTimeouts = reminderService.processCitizenReviewTimeouts();

        // 5. Recalculate health scores
        int healthScores = healthScoreService.recalculateAll();

        return new CycleResult(reminders, overdue, escalations, healthScores, now, demoMode);
    }

    public static class CycleResult {
        private final int remindersDispatched;
        private final int overdueTransitioned;
        private final int escalationsProcessed;
        private final int healthScoresRecalculated;
        private final LocalDateTime executedAt;
        private final boolean demoMode;

        public CycleResult(int remindersDispatched, int overdueTransitioned, int escalationsProcessed, int healthScoresRecalculated, LocalDateTime executedAt, boolean demoMode) {
            this.remindersDispatched = remindersDispatched;
            this.overdueTransitioned = overdueTransitioned;
            this.escalationsProcessed = escalationsProcessed;
            this.healthScoresRecalculated = healthScoresRecalculated;
            this.executedAt = executedAt;
            this.demoMode = demoMode;
        }

        public int getRemindersDispatched() { return remindersDispatched; }
        public int getOverdueTransitioned() { return overdueTransitioned; }
        public int getEscalationsProcessed() { return escalationsProcessed; }
        public int getHealthScoresRecalculated() { return healthScoresRecalculated; }
        public LocalDateTime getExecutedAt() { return executedAt; }
        public boolean isDemoMode() { return demoMode; }

        @Override
        public String toString() {
            return String.format("CycleResult[reminders=%d, overdue=%d, escalations=%d, healthScores=%d, executedAt=%s, demoMode=%b]",
                    remindersDispatched, overdueTransitioned, escalationsProcessed, healthScoresRecalculated, executedAt, demoMode);
        }
    }
}

