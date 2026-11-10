package com.resolvetrack.analytics;

import com.resolvetrack.auth.CustomUserDetails;
import com.resolvetrack.auth.Role;
import com.resolvetrack.auth.User;
import com.resolvetrack.complaint.*;
import com.resolvetrack.department.Department;
import com.resolvetrack.department.DepartmentRepository;
import com.resolvetrack.escalation.EscalationRepository;
import com.resolvetrack.officer.Officer;
import com.resolvetrack.officer.OfficerRepository;
import com.resolvetrack.reminder.HealthBand;
import com.resolvetrack.reminder.HealthScoreResult;
import com.resolvetrack.reminder.HealthScoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsAndBriefingTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private OfficerRepository officerRepository;

    @Mock
    private EscalationRepository escalationRepository;

    @Mock
    private NextBestActionService nextBestActionService;

    @Mock
    private HealthScoreService healthScoreService;

    private Clock fixedClock;
    private DashboardService dashboardService;

    private Department deptBBMP;
    private Department deptPolice;
    private Officer officerRamesh;
    private CustomUserDetails supervisorUser;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-11-09T09:00:00Z"), ZoneId.of("UTC"));
        dashboardService = new DashboardService(
                complaintRepository,
                departmentRepository,
                officerRepository,
                escalationRepository,
                nextBestActionService,
                healthScoreService,
                fixedClock
        );

        deptBBMP = new Department();
        deptBBMP.setId(4L);
        deptBBMP.setCode("MUN");
        deptBBMP.setNameEn("Municipal Administration (BBMP)");
        deptBBMP.setNameKn("ನಗರ ಆಡಳಿತ");
        deptBBMP.setDefaultSlaDays(7);

        deptPolice = new Department();
        deptPolice.setId(1L);
        deptPolice.setCode("POL");
        deptPolice.setNameEn("Police Department");
        deptPolice.setNameKn("ಪೊಲೀಸ್ ಇಲಾಖೆ");
        deptPolice.setDefaultSlaDays(7);

        User officerUser = new User();
        officerUser.setId(7L);
        officerUser.setFullName("Health Inspector Manjunath");

        officerRamesh = new Officer();
        officerRamesh.setId(4L);
        officerRamesh.setUser(officerUser);
        officerRamesh.setDepartment(deptBBMP);
        officerRamesh.setEmployeeCode("EMP-MUN-001");

        User sup = new User();
        sup.setId(3L);
        sup.setUsername("supervisor_muni");
        sup.setFullName("Executive Engineer Suresh");
        sup.setRole(Role.SUPERVISOR);
        sup.setDepartment(deptBBMP);
        supervisorUser = new CustomUserDetails(sup);
    }

    @Test
    @DisplayName("Prompt 8.1: /api/dashboard/analytics aggregates average resolution, overdue percentage, workload, and area heatmap")
    void testGetAnalyticsAggregatesCorrectly() {
        LocalDateTime now = LocalDateTime.now(fixedClock);

        // Complaint 1: Resolved in 24 hours
        Complaint c1 = new Complaint();
        c1.setId(1L);
        c1.setStatus(ComplaintStatus.RESOLVED);
        c1.setCreatedAt(now.minusHours(24));
        c1.setUpdatedAt(now);
        c1.setDepartment(deptBBMP);
        c1.setAssignedOfficer(officerRamesh);
        c1.setLocationAddress("8th Main Rd, Vidyaranyapura, Bengaluru");

        // Complaint 2: Overdue, Reopened, Critical
        Complaint c2 = new Complaint();
        c2.setId(2L);
        c2.setStatus(ComplaintStatus.REOPENED);
        c2.setCreatedAt(now.minusDays(5));
        c2.setFollowUpDeadline(now.minusHours(12)); // overdue
        c2.setReopenCount(1);
        c2.setEscalationLevel(1);
        c2.setHealthScore(35);
        c2.setDepartment(deptBBMP);
        c2.setAssignedOfficer(officerRamesh);
        c2.setLocationAddress("8th Main Rd, Vidyaranyapura, Bengaluru");

        when(complaintRepository.findAll()).thenReturn(List.of(c1, c2));
        when(departmentRepository.findAllByActiveTrue()).thenReturn(List.of(deptBBMP, deptPolice));
        when(officerRepository.findAll()).thenReturn(List.of(officerRamesh));

        AnalyticsDashboardDto analytics = dashboardService.getAnalytics(supervisorUser);

        assertThat(analytics).isNotNull();
        assertThat(analytics.getTotalCount()).isEqualTo(2);
        assertThat(analytics.getResolvedCount()).isEqualTo(1);
        assertThat(analytics.getPendingCount()).isEqualTo(1);
        assertThat(analytics.getOverduePercentage()).isEqualTo(50.0);
        assertThat(analytics.getReopenedCount()).isEqualTo(1);
        assertThat(analytics.getEscalationCount()).isEqualTo(1);
        assertThat(analytics.getAverageResolutionTimeHours()).isEqualTo(24.0);

        // Check officer workload
        assertThat(analytics.getOfficerWorkload()).hasSize(1);
        AnalyticsDashboardDto.OfficerWorkloadDto workload = analytics.getOfficerWorkload().get(0);
        assertThat(workload.getOfficerName()).isEqualTo("Health Inspector Manjunath");
        assertThat(workload.getActiveCount()).isEqualTo(1);
        assertThat(workload.getOverdueCount()).isEqualTo(1);
        assertThat(workload.getResolvedCount()).isEqualTo(1);

        // Check overdue by area heatmap (aggregated Leaflet circles)
        assertThat(analytics.getOverdueByArea()).isNotEmpty();
        AnalyticsDashboardDto.AreaOverdueDto vidya = analytics.getOverdueByArea().stream()
                .filter(a -> a.getAreaName().contains("Vidyaranyapura"))
                .findFirst().orElseThrow();
        assertThat(vidya.getComplaintCount()).isEqualTo(2);
        assertThat(vidya.getOverdueCount()).isEqualTo(1);
        assertThat(vidya.getCriticalCount()).isEqualTo(1);
        assertThat(vidya.getRadiusMeters()).isGreaterThanOrEqualTo(500);
    }

    @Test
    @DisplayName("Prompt 8.1: /api/dashboard/briefing generates morning summary for supervisor with template and top-priority complaint")
    void testGetSupervisorBriefingGeneratesBriefAndTopPriority() {
        LocalDateTime now = LocalDateTime.now(fixedClock);

        Complaint top = new Complaint();
        top.setId(8L);
        top.setTrackingId("RSK-2026-008");
        top.setTitle("Major Water Pipe Burst Flooding 8th Main");
        top.setCategory("Water & Drainage");
        top.setPriority(Priority.URGENT);
        top.setStatus(ComplaintStatus.REOPENED);
        top.setHealthScore(30);
        top.setFollowUpDeadline(now.minusHours(5)); // overdue
        top.setDepartment(deptBBMP);
        top.setLocationAddress("Vidyaranyapura, Bengaluru");

        when(complaintRepository.findAll()).thenReturn(List.of(top));
        when(nextBestActionService.determineNextBestAction(eq(top), any(LocalDateTime.class)))
                .thenReturn(NextBestAction.of("ESCALATE", "Escalate Grievance", "Repeated delays or citizen dispute", "SUPERVISOR", "CRITICAL"));

        SupervisorBriefingDto briefing = dashboardService.getSupervisorBriefing(supervisorUser);

        assertThat(briefing).isNotNull();
        assertThat(briefing.getPendingCount()).isEqualTo(1);
        assertThat(briefing.getOverdueCount()).isEqualTo(1);
        assertThat(briefing.getReopenedCount()).isEqualTo(1);
        assertThat(briefing.getNarrativeEn()).contains("Executive Engineer Suresh");
        assertThat(briefing.getNarrativeEn()).contains("Karnataka SAKALA Services Act");
        assertThat(briefing.getNarrativeKn()).contains("ಶುಭೋದಯ");

        // Top Priority Complaint
        assertThat(briefing.getTopPriorityComplaint()).isNotNull();
        assertThat(briefing.getTopPriorityComplaint().getTrackingId()).isEqualTo("RSK-2026-008");
        assertThat(briefing.getTopPriorityComplaint().getPriority()).isEqualTo("URGENT");
        assertThat(briefing.getTopPriorityComplaint().getHealthBand()).isEqualTo("CRITICAL");
        assertThat(briefing.getTopPriorityComplaint().getRecommendedAction()).contains("Repeated delays or citizen dispute");
    }
}

