package com.resolvetrack.escalation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(EscalationScheduler.class);

    private final EscalationService escalationService;

    public EscalationScheduler(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @Scheduled(cron = "${resolvetrack.scheduling.escalation-cron:0 0 */4 * * *}")
    public void runStatutoryEscalations() {
        log.info("Starting statutory SLA compliance audit and escalation scan...");
        int count = escalationService.processStatutoryEscalations();
        log.info("Statutory escalation scan complete. Total complaints escalated: {}", count);
    }
}
