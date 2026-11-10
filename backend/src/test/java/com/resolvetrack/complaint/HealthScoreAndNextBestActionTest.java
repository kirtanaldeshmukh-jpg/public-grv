package com.resolvetrack.complaint;

import com.resolvetrack.auth.CustomUserDetails;
import com.resolvetrack.auth.Role;
import com.resolvetrack.auth.User;
import com.resolvetrack.common.SystemSetting;
import com.resolvetrack.common.SystemSettingRepository;
import com.resolvetrack.department.Department;
import com.resolvetrack.history.ActionHistory;
import com.resolvetrack.history.ActionHistoryRepository;
import com.resolvetrack.officer.Officer;
import com.resolvetrack.officer.OfficerRepository;
import com.resolvetrack.reminder.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class HealthScoreAndNextBestActionTest {

    private ComplaintRepository complaintRepository;
    private ActionHistoryRepository actionHistoryRepository;
    private ReminderRepository reminderRepository;
    private SystemSettingRepository systemSettingRepository;
    private OfficerRepository officerRepository;
    private Clock fixedClock;
    private LocalDateTime fixedNow;

    private HealthScoreService healthScoreService;
    private NextBestActionService nextBestActionService;

    @BeforeEach
    void setUp() {
        complaintRepository = Mockito.mock(ComplaintRepository.class);
        actionHistoryRepository = Mockito.mock(ActionHistoryRepository.class);
        reminderRepository = Mockito.mock(ReminderRepository.class);
        systemSettingRepository = Mockito.mock(SystemSettingRepository.class);
        officerRepository = Mockito.mock(OfficerRepository.class);

        fixedNow = LocalDateTime.of(2026, 11, 8, 10, 0, 0);
        fixedClock = Clock.fixed(fixedNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

        when(systemSettingRepository.findBySettingKey("reminder.lead_time"))
                .thenReturn(Optional.of(new SystemSetting(1L, "reminder.lead_time", "24", "Lead time", fixedNow)));

        healthScoreService = new HealthScoreService(
                complaintRepository, reminderRepository, actionHistoryRepository, fixedClock);

        nextBestActionService = new NextBestActionService(
                complaintRepository, actionHistoryRepository, reminderRepository,
                systemSettingRepository, officerRepository, fixedClock);
    }

    @Test
    @DisplayName("Prompt 7.1: Health score detail returns score, band, and reasons")
    void testHealthScoreDetailAndBands() {
        Department dept = Department.builder().id(1L).nameEn("Police").build();
        Officer officer = Officer.builder().id(10L).employeeCode("POL-01").build();

        // 1. Healthy complaint on track
        Complaint healthy = Complaint.builder()
                .id(1L)
                .trackingId("RSK-2026-001")
                .department(dept)
                .assignedOfficer(officer)
                .status(ComplaintStatus.IN_PROGRESS)
                .followUpDeadline(fixedNow.plusDays(3))
                .build();

        HealthScoreResult res1 = healthScoreService.calculateHealthDetail(healthy, fixedNow);
        assertEquals(100, res1.getScore());
        assertEquals(HealthBand.HEALTHY, res1.getBand());
        assertFalse(res1.getReasons().isEmpty());

        // 2. Overdue with unacked reminders -> Critical band
        Complaint critical = Complaint.builder()
                .id(2L)
                .trackingId("RSK-2026-002")
                .department(dept)
                .assignedOfficer(officer)
                .status(ComplaintStatus.OVERDUE)
                .followUpDeadline(fixedNow.minusHours(5))
                .escalationLevel(1)
                .reopenCount(1)
                .build();

        when(reminderRepository.countByComplaintIdAndAcknowledgedFalse(2L)).thenReturn(2L);

        HealthScoreResult res2 = healthScoreService.calculateHealthDetail(critical, fixedNow);
        assertTrue(res2.getScore() < 50, "Score should be critical: " + res2.getScore());
        assertEquals(HealthBand.CRITICAL, res2.getBand());
        assertTrue(res2.getReasons().stream().anyMatch(r -> r.contains("SLA deadline breached")));
        assertTrue(res2.getReasons().stream().anyMatch(r -> r.contains("Level 1 statutory escalation")));
        assertTrue(res2.getReasons().stream().anyMatch(r -> r.contains("unacknowledged reminder")));
        assertTrue(res2.getReasons().stream().anyMatch(r -> r.contains("Reopened 1 time")));
    }

    @Test
    @DisplayName("Prompt 7.1 Rule 1: Resolution submitted -> Request citizen verification")
    void testRuleResolutionSubmitted() {
        Complaint c = Complaint.builder()
                .id(10L)
                .status(ComplaintStatus.RESOLUTION_SUBMITTED)
                .resolutionSummary("Fixed streetlight wire")
                .build();

        NextBestAction nba = nextBestActionService.determineNextBestAction(c, fixedNow);
        assertEquals("REQUEST_CITIZEN_VERIFICATION", nba.getAction());
        assertEquals("CITIZEN", nba.getTargetRole());
        assertTrue(nba.getReason().contains("Request citizen sign-off"));
    }

    @Test
    @DisplayName("Prompt 7.1 Rule 2: Unassigned -> Assign officer")
    void testRuleUnassigned() {
        Complaint c = Complaint.builder()
                .id(11L)
                .status(ComplaintStatus.NEW)
                .assignedOfficer(null)
                .build();

        NextBestAction nba = nextBestActionService.determineNextBestAction(c, fixedNow);
        assertEquals("ASSIGN_OFFICER", nba.getAction());
        assertEquals("SUPERVISOR", nba.getTargetRole());
        assertTrue(nba.getReason().contains("unassigned"));
    }

    @Test
    @DisplayName("Prompt 7.1 Rule 3: Repeated delays or disputes -> Escalate")
    void testRuleRepeatedDelaysEscalate() {
        Officer officer = Officer.builder().id(5L).build();
        Complaint c = Complaint.builder()
                .id(12L)
                .status(ComplaintStatus.OVERDUE)
                .assignedOfficer(officer)
                .reopenCount(2)
                .followUpDeadline(fixedNow.minusDays(1))
                .build();

        NextBestAction nba = nextBestActionService.determineNextBestAction(c, fixedNow);
        assertEquals("ESCALATE", nba.getAction());
        assertEquals("SUPERVISOR", nba.getTargetRole());
        assertTrue(nba.getReason().contains("Repeated delays or citizen dispute"));
    }

    @Test
    @DisplayName("Prompt 7.1 Rule 4: Overdue and no recent update -> Notify supervisor")
    void testRuleOverdueNoUpdateNotifySupervisor() {
        Officer officer = Officer.builder().id(5L).build();
        Complaint c = Complaint.builder()
                .id(13L)
                .status(ComplaintStatus.OVERDUE)
                .assignedOfficer(officer)
                .followUpDeadline(fixedNow.minusHours(12))
                .build();

        when(actionHistoryRepository.findAllByComplaintIdOrderByCreatedAtAsc(13L))
                .thenReturn(Collections.emptyList());

        NextBestAction nba = nextBestActionService.determineNextBestAction(c, fixedNow);
        assertEquals("NOTIFY_SUPERVISOR", nba.getAction());
        assertEquals("OFFICER", nba.getTargetRole());
        assertTrue(nba.getReason().contains("Alert departmental supervisor"));
    }

    @Test
    @DisplayName("Prompt 7.1 Rule 5: Deadline within lead time -> Send reminder")
    void testRuleDeadlineWithinLeadTime() {
        Officer officer = Officer.builder().id(5L).build();
        Complaint c = Complaint.builder()
                .id(14L)
                .status(ComplaintStatus.IN_PROGRESS)
                .assignedOfficer(officer)
                .followUpDeadline(fixedNow.plusHours(12)) // within 24h lead time
                .build();

        NextBestAction nba = nextBestActionService.determineNextBestAction(c, fixedNow);
        assertEquals("SEND_REMINDER", nba.getAction());
        assertEquals("OFFICER", nba.getTargetRole());
        assertTrue(nba.getReason().contains("approaching within lead window"));
    }

    @Test
    @DisplayName("Prompt 7.1 Rule 6: Missing documents -> Request documents")
    void testRuleMissingDocuments() {
        Officer officer = Officer.builder().id(5L).build();
        Complaint c = Complaint.builder()
                .id(15L)
                .status(ComplaintStatus.IN_PROGRESS)
                .assignedOfficer(officer)
                .title("Need electricity connection")
                .description("Please verify bill copy and ration card proof for meter installation")
                .followUpDeadline(fixedNow.plusDays(4))
                .attachments(Collections.emptyList())
                .build();

        NextBestAction nba = nextBestActionService.determineNextBestAction(c, fixedNow);
        assertEquals("REQUEST_DOCUMENTS", nba.getAction());
        assertEquals("CITIZEN", nba.getTargetRole());
        assertTrue(nba.getReason().contains("Request supplementary proofs"));
    }

    @Test
    @DisplayName("Prompt 7.1: Grouped recommended actions per officer summary")
    void testRecommendedActionsSummaryGrouping() {
        Department dept = Department.builder().id(1L).nameEn("Electricity").build();
        User officerUser = User.builder().id(20L).fullName("Officer Priya").role(Role.OFFICER).build();
        Officer officer = Officer.builder().id(7L).user(officerUser).build();

        Complaint c1 = Complaint.builder()
                .id(101L)
                .trackingId("RSK-2026-101")
                .title("Wire spark")
                .department(dept)
                .assignedOfficer(officer)
                .status(ComplaintStatus.OVERDUE)
                .followUpDeadline(fixedNow.minusHours(4))
                .build();

        Complaint c2 = Complaint.builder()
                .id(102L)
                .trackingId("RSK-2026-102")
                .title("Low voltage")
                .department(dept)
                .assignedOfficer(officer)
                .status(ComplaintStatus.IN_PROGRESS)
                .followUpDeadline(fixedNow.plusHours(6))
                .build();

        when(complaintRepository.findAll()).thenReturn(List.of(c1, c2));

        CustomUserDetails adminUser = new CustomUserDetails(User.builder().id(1L).role(Role.ADMIN).build());
        NextBestActionService.RecommendedActionsSummaryDto summary =
                nextBestActionService.getRecommendedActionsSummary(adminUser);

        assertEquals(1, summary.totalOverdue());
        assertEquals(1, summary.totalApproachingDeadline());
        assertEquals(1, summary.officerGroups().size());

        NextBestActionService.OfficerRecommendedGroupDto group = summary.officerGroups().get(0);
        assertEquals(7L, group.officerId());
        assertEquals("Officer Priya", group.officerName());
        assertEquals(1, group.overdueCount());
        assertEquals(1, group.approachingDeadlineCount());
        assertEquals(2, group.actions().size());
    }
}
