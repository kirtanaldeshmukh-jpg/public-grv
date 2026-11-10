package com.resolvetrack.reminder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderService reminderService;

    public ReminderScheduler(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(cron = "${resolvetrack.scheduling.reminder-cron:0 0 9 * * *}")
    public void runAutomatedReminders() {
        log.info("Starting scheduled reminder nudge dispatch...");
        int count = reminderService.dispatchPreDeadlineNudges();
        log.info("Automated reminder nudge run complete. Processed count: {}", count);
    }
}
