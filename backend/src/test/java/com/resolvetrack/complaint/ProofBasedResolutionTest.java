package com.resolvetrack.complaint;

import com.resolvetrack.auth.Role;
import com.resolvetrack.auth.User;
import com.resolvetrack.auth.UserRepository;
import com.resolvetrack.common.BusinessRuleException;
import com.resolvetrack.common.SystemSetting;
import com.resolvetrack.common.SystemSettingRepository;
import com.resolvetrack.department.Department;
import com.resolvetrack.department.DepartmentRepository;
import com.resolvetrack.history.ActionHistoryService;
import com.resolvetrack.notification.NotificationService;
import com.resolvetrack.officer.Officer;
import com.resolvetrack.officer.OfficerRepository;
import com.resolvetrack.reminder.HealthBand;
import com.resolvetrack.reminder.HealthScoreResult;
import com.resolvetrack.reminder.HealthScoreService;
import com.resolvetrack.reminder.ReminderRepository;
import com.resolvetrack.reminder.ReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProofBasedResolutionTest {

    private ComplaintRepository complaintRepository;
    private DepartmentRepository departmentRepository;
    private UserRepository userRepository;
    private OfficerRepository officerRepository;
    private ActionHistoryService actionHistoryService;
    private FileUploadService fileUploadService;
    private NotificationService notificationService;
    private SystemSettingRepository systemSettingRepository;
    private HealthScoreService healthScoreService;
    private ReminderRepository reminderRepository;
    private ReminderService reminderService;
    private Clock fixedClock;
    private LocalDateTime fixedNow;

    private ComplaintService complaintService;

    @BeforeEach
    void setUp() {
        complaintRepository = mock(ComplaintRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        userRepository = mock(UserRepository.class);
        officerRepository = mock(OfficerRepository.class);
        actionHistoryService = mock(ActionHistoryService.class);
        fileUploadService = mock(FileUploadService.class);
        notificationService = mock(NotificationService.class);
        systemSettingRepository = mock(SystemSettingRepository.class);
        healthScoreService = mock(HealthScoreService.class);
        reminderRepository = mock(ReminderRepository.class);

        fixedNow = LocalDateTime.of(2026, 11, 8, 12, 0, 0);
        fixedClock = Clock.fixed(fixedNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

        when(systemSettingRepository.findBySettingKey("resolution.citizen.review.timeout.days"))
                .thenReturn(Optional.of(new SystemSetting(1L, "resolution.citizen.review.timeout.days", "3", "Timeout days", fixedNow)));
        when(systemSettingRepository.findBySettingKey("demo.mode"))
                .thenReturn(Optional.of(new SystemSetting(2L, "demo.mode", "false", "Demo mode", fixedNow)));

        complaintService = new ComplaintService(
                complaintRepository,
                departmentRepository,
                userRepository,
                officerRepository,
                actionHistoryService,
                null,
                null,
                fileUploadService,
                null,
                notificationService,
                systemSettingRepository,
                healthScoreService,
                reminderRepository,
                null,
                null,
                null,
                null,
                fixedClock
        );

        when(healthScoreService.calculateHealthDetail(any(), any()))
                .thenReturn(new HealthScoreResult(100, HealthBand.HEALTHY, List.of("Initial Healthy Score")));

        reminderService = new ReminderService(
                reminderRepository,
                complaintRepository,
                actionHistoryService,
                systemSettingRepository,
                notificationService,
                userRepository,
                fixedClock
        );
    }

    @Test
    @DisplayName("Prompt 7.3: Officer submits resolution -> transitions to RESOLUTION_SUBMITTED")
    void testOfficerSubmitsResolution() {
        User officerUser = User.builder().id(5L).username("officer_police").fullName("Officer Rajesh").role(Role.OFFICER).build();
        when(userRepository.findByUsername("officer_police")).thenReturn(Optional.of(officerUser));

        Department dept = Department.builder().id(1L).nameEn("Police").build();
        Officer officer = Officer.builder().id(2L).user(officerUser).build();
        User citizen = User.builder().id(10L).fullName("Citizen Suresh").phoneNumber("+91 9876543210").build();

        Complaint complaint = Complaint.builder()
                .id(200L)
                .trackingId("RSK-2026-200")
                .status(ComplaintStatus.IN_PROGRESS)
                .department(dept)
                .assignedOfficer(officer)
                .citizen(citizen)
                .followUpDeadline(LocalDateTime.now().plusDays(2))
                .slaDueDate(LocalDateTime.now().plusDays(5))
                .attachments(new ArrayList<>())
                .build();

        when(complaintRepository.findById(200L)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(i -> i.getArgument(0));

        ComplaintDto.SubmitResolutionRequest req = new ComplaintDto.SubmitResolutionRequest(
                "Recovered stolen bicycle and handed over receipt proof");

        ComplaintDto.OfficerDetailResponse res = complaintService.submitResolution(200L, req, Collections.emptyList(), "officer_police");

        assertEquals(ComplaintStatus.RESOLUTION_SUBMITTED, complaint.getStatus());
        assertEquals("Recovered stolen bicycle and handed over receipt proof", complaint.getResolutionSummary());
        assertNotNull(complaint.getResolutionSubmittedAt());

        verify(actionHistoryService, times(1)).logAction(
                eq(200L), eq(5L), eq("Officer Rajesh"), eq("OFFICER"), eq("RESOLUTION_SUBMITTED"),
                eq("IN_PROGRESS"), eq("RESOLUTION_SUBMITTED"), contains("Recovered stolen bicycle"), eq(false)
        );
    }

    @Test
    @DisplayName("Prompt 7.3: Citizen confirms resolution -> transitions to RESOLVED")
    void testCitizenConfirmsResolution() {
        Department dept = Department.builder().id(1L).nameEn("Police").build();
        User citizen = User.builder().id(10L).fullName("Citizen Suresh").build();

        Complaint complaint = Complaint.builder()
                .id(201L)
                .trackingId("RSK-2026-201")
                .status(ComplaintStatus.RESOLUTION_SUBMITTED)
                .department(dept)
                .citizen(citizen)
                .resolutionSummary("Fixed streetlight")
                .attachments(new ArrayList<>())
                .build();

        when(complaintRepository.findByTrackingIdOrTrackingNumber("RSK-2026-201", "RSK-2026-201"))
                .thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(i -> i.getArgument(0));

        ComplaintDto.ConfirmResolutionRequest req = new ComplaintDto.ConfirmResolutionRequest("Very satisfied with the quick fix!");
        ComplaintDto.CitizenTrackingResponse res = complaintService.confirmResolutionByCitizen("RSK-2026-201", req);

        assertEquals(ComplaintStatus.RESOLVED, complaint.getStatus());
        verify(actionHistoryService, times(1)).logAction(
                eq(201L), eq(10L), eq("Citizen Suresh"), eq("CITIZEN"), eq("RESOLUTION_CONFIRMED"),
                eq("RESOLUTION_SUBMITTED"), eq("RESOLVED"), contains("Very satisfied"), eq(false)
        );
    }

    @Test
    @DisplayName("Prompt 7.3: Citizen rejects resolution -> transitions to REOPENED, increments reopen_count, alerts supervisor")
    void testCitizenRejectsResolution() {
        Department dept = Department.builder().id(1L).nameEn("Police").build();
        User citizen = User.builder().id(10L).fullName("Citizen Suresh").build();
        User supervisor = User.builder().id(99L).username("supervisor_police").fullName("Supervisor Ramesh").role(Role.SUPERVISOR).departmentId(1L).build();

        when(userRepository.findAll()).thenReturn(List.of(supervisor));

        Complaint complaint = Complaint.builder()
                .id(202L)
                .trackingId("RSK-2026-202")
                .status(ComplaintStatus.RESOLUTION_SUBMITTED)
                .department(dept)
                .citizen(citizen)
                .reopenCount(0)
                .resolutionSummary("Checked area, no light bulb issue")
                .followUpDeadline(LocalDateTime.now().plusDays(2))
                .slaDueDate(LocalDateTime.now().plusDays(5))
                .attachments(new ArrayList<>())
                .build();

        when(complaintRepository.findByTrackingIdOrTrackingNumber("RSK-2026-202", "RSK-2026-202"))
                .thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(i -> i.getArgument(0));

        ComplaintDto.RejectResolutionRequest req = new ComplaintDto.RejectResolutionRequest("Streetlight is still broken and sparking at night!");
        ComplaintDto.CitizenTrackingResponse res = complaintService.rejectResolutionByCitizen("RSK-2026-202", req);

        assertEquals(ComplaintStatus.REOPENED, complaint.getStatus());
        assertEquals(1, complaint.getReopenCount());

        verify(actionHistoryService, times(1)).logAction(
                eq(202L), eq(10L), eq("Citizen Suresh"), eq("CITIZEN"), eq("RESOLUTION_REJECTED"),
                eq("RESOLUTION_SUBMITTED"), eq("REOPENED"), contains("still broken and sparking"), eq(false)
        );

        // Verify supervisor notified
        verify(notificationService, times(1)).sendInAppNotification(
                eq(supervisor), eq(complaint), contains("Grievance Reopened"), contains("Reopen #1")
        );
    }

    @Test
    @DisplayName("Prompt 7.3: Inactive citizen does NOT auto-resolve; flags for supervisor review")
    void testCitizenInactivityTimeoutFlagsSupervisorReview() {
        Department dept = Department.builder().id(1L).nameEn("Police").build();
        User supervisor = User.builder().id(99L).username("supervisor_police").role(Role.SUPERVISOR).departmentId(1L).build();
        when(userRepository.findAll()).thenReturn(List.of(supervisor));

        Complaint unverifiedOld = Complaint.builder()
                .id(203L)
                .trackingId("RSK-2026-203")
                .status(ComplaintStatus.RESOLUTION_SUBMITTED)
                .department(dept)
                .resolutionSubmittedAt(fixedNow.minusDays(5)) // Submitted 5 days ago, timeout is 3 days
                .build();

        when(complaintRepository.findAll()).thenReturn(List.of(unverifiedOld));
        when(actionHistoryService.getInternalHistory(203L)).thenReturn(Collections.emptyList());

        int flagged = reminderService.processCitizenReviewTimeouts();

        assertEquals(1, flagged);
        // Status remains RESOLUTION_SUBMITTED (never auto-resolved without explicit human verification)
        assertEquals(ComplaintStatus.RESOLUTION_SUBMITTED, unverifiedOld.getStatus());

        verify(actionHistoryService, times(1)).logAction(
                eq(203L), any(), eq("SYSTEM"), eq("SYSTEM"), eq("REVIEW_TIMEOUT_SUPERVISOR_FLAGGED"),
                eq("RESOLUTION_SUBMITTED"), eq("RESOLUTION_SUBMITTED"), contains("supervisor sign-off"), eq(true)
        );

        verify(notificationService, times(1)).sendInAppNotification(
                eq(supervisor), eq(unverifiedOld), contains("Citizen Review Timeout"), anyString()
        );
    }
}
