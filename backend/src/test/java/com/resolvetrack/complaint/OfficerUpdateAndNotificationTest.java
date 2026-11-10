package com.resolvetrack.complaint;

import com.resolvetrack.auth.CustomUserDetails;
import com.resolvetrack.auth.Role;
import com.resolvetrack.auth.User;
import com.resolvetrack.auth.UserRepository;
import com.resolvetrack.common.ResourceNotFoundException;
import com.resolvetrack.common.SystemSetting;
import com.resolvetrack.common.SystemSettingRepository;
import com.resolvetrack.department.Department;
import com.resolvetrack.department.DepartmentRepository;
import com.resolvetrack.department.RuleBasedDepartmentSuggestionService;
import com.resolvetrack.escalation.Escalation;
import com.resolvetrack.escalation.EscalationRepository;
import com.resolvetrack.history.ActionHistory;
import com.resolvetrack.history.ActionHistoryDto;
import com.resolvetrack.history.ActionHistoryRepository;
import com.resolvetrack.history.ActionHistoryService;
import com.resolvetrack.notification.Notification;
import com.resolvetrack.notification.NotificationDto;
import com.resolvetrack.notification.NotificationRepository;
import com.resolvetrack.notification.NotificationService;
import com.resolvetrack.officer.Officer;
import com.resolvetrack.officer.OfficerRepository;
import com.resolvetrack.reminder.HealthScoreService;
import com.resolvetrack.reminder.Reminder;
import com.resolvetrack.reminder.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class OfficerUpdateAndNotificationTest {

    private ComplaintService complaintService;
    private ComplaintSecurityService complaintSecurityService;
    private NotificationService notificationService;
    private ActionHistoryService actionHistoryService;
    private HealthScoreService healthScoreService;

    private InMemoryComplaintRepository complaintRepository;
    private InMemoryUserRepository userRepository;
    private InMemoryDepartmentRepository departmentRepository;
    private InMemoryOfficerRepository officerRepository;
    private InMemoryActionHistoryRepository actionHistoryRepository;
    private InMemoryReminderRepository reminderRepository;
    private InMemoryEscalationRepository escalationRepository;
    private InMemoryNotificationRepository notificationRepository;
    private InMemorySystemSettingRepository systemSettingRepository;

    private User officerUser1;
    private User officerUser2;
    private User supervisorUser;
    private User adminUser;
    private User citizenUser;
    private Officer officer1;
    private Officer officer2;
    private Department policeDept;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-05T10:00:00Z"), ZoneId.of("Asia/Kolkata"));

    @BeforeEach
    void setUp() {
        complaintRepository = new InMemoryComplaintRepository();
        userRepository = new InMemoryUserRepository();
        departmentRepository = new InMemoryDepartmentRepository();
        officerRepository = new InMemoryOfficerRepository();
        actionHistoryRepository = new InMemoryActionHistoryRepository();
        reminderRepository = new InMemoryReminderRepository();
        escalationRepository = new InMemoryEscalationRepository();
        notificationRepository = new InMemoryNotificationRepository();
        systemSettingRepository = new InMemorySystemSettingRepository();

        notificationService = new NotificationService(notificationRepository, clock);
        actionHistoryService = new ActionHistoryService(actionHistoryRepository);
        healthScoreService = new HealthScoreService(complaintRepository, reminderRepository, actionHistoryRepository, clock);

        complaintSecurityService = new ComplaintSecurityService(complaintRepository);

        TrackingIdGeneratorService trackingService = new TrackingIdGeneratorService(new InMemoryTrackingSequenceRepository());
        FileUploadService fileUploadService = new FileUploadService("./uploads");
        RuleBasedDepartmentSuggestionService suggestionService = new RuleBasedDepartmentSuggestionService(departmentRepository);

        complaintService = new ComplaintService(
                complaintRepository,
                departmentRepository,
                userRepository,
                officerRepository,
                actionHistoryService,
                actionHistoryRepository,
                trackingService,
                fileUploadService,
                suggestionService,
                notificationService,
                systemSettingRepository,
                healthScoreService,
                reminderRepository,
                escalationRepository,
                clock
        );

        // Seed department
        policeDept = new Department(1L, "POL", "Police Department", "ಪೊಲೀಸ್ ಇಲಾಖೆ", "Law & Order", 7, 24, true);
        departmentRepository.save(policeDept);

        // Seed users
        officerUser1 = User.builder().id(11L).username("officer1").fullName("Inspector Rajesh").role(Role.OFFICER).departmentId(1L).build();
        officerUser2 = User.builder().id(12L).username("officer2").fullName("Inspector Suresh").role(Role.OFFICER).departmentId(1L).build();
        supervisorUser = User.builder().id(13L).username("supervisor1").fullName("Superintendent Ramesh").role(Role.SUPERVISOR).departmentId(1L).build();
        adminUser = User.builder().id(14L).username("admin").fullName("System Administrator").role(Role.ADMIN).build();
        citizenUser = User.builder().id(15L).username("citizen1").fullName("Citizen Anand").role(Role.CITIZEN).build();

        userRepository.save(officerUser1);
        userRepository.save(officerUser2);
        userRepository.save(supervisorUser);
        userRepository.save(adminUser);
        userRepository.save(citizenUser);

        officer1 = Officer.builder().id(21L).user(officerUser1).department(policeDept).employeeCode("EMP-POL-001").designation("CPI").build();
        officer2 = Officer.builder().id(22L).user(officerUser2).department(policeDept).employeeCode("EMP-POL-002").designation("PSI").build();
        officerRepository.save(officer1);
        officerRepository.save(officer2);
    }

    private Complaint createAssignedComplaint(String trackingId) {
        Complaint c = Complaint.builder()
                .trackingId(trackingId)
                .trackingNumber(trackingId)
                .citizen(citizenUser)
                .department(policeDept)
                .assignedOfficer(officer1)
                .title("Vehicle Theft " + trackingId)
                .description("Two wheeler stolen from Indiranagar")
                .category("theft")
                .status(ComplaintStatus.ASSIGNED)
                .followUpDeadline(LocalDateTime.now(clock).plusDays(2))
                .slaDueDate(LocalDateTime.now(clock).plusDays(2))
                .escalationLevel(0)
                .healthScore(80)
                .build();
        return complaintRepository.save(c);
    }

    @Test
    @DisplayName("Officer Update: Resets follow-up deadline, acknowledges open reminders, recalculates health score")
    void testOfficerUpdate_SuccessFlow() {
        Complaint complaint = createAssignedComplaint("RSK-2026-UPDATE-1");

        // Seed an open reminder
        Reminder openReminder = Reminder.builder()
                .complaint(complaint)
                .officer(officer1)
                .reminderType("PRE_DEADLINE_NUDGE")
                .message("Deadline approaching")
                .acknowledged(false)
                .build();
        reminderRepository.save(openReminder);
        assertEquals(1, reminderRepository.countByComplaintIdAndAcknowledgedFalse(complaint.getId()));

        // Prepare update request
        LocalDateTime nextDate = LocalDateTime.now(clock).plusDays(5);
        ComplaintDto.ProgressUpdateRequest request = new ComplaintDto.ProgressUpdateRequest(
                ComplaintStatus.IN_PROGRESS,
                "FIR registered and CCTV footage retrieved from nearby junction.",
                true, // public for citizen timeline
                nextDate
        );

        // Execute update as assigned officer
        ComplaintDto.OfficerDetailResponse response = complaintService.updateProgress(complaint.getId(), request, "officer1");

        assertNotNull(response);
        assertEquals(ComplaintStatus.IN_PROGRESS, response.getStatus());
        assertEquals(nextDate, response.getFollowUpDeadline());

        // Verify open reminder was acknowledged
        Reminder updatedReminder = reminderRepository.findById(openReminder.getId()).get();
        assertTrue(updatedReminder.isAcknowledged());
        assertNotNull(updatedReminder.getAcknowledgedAt());
        assertEquals(0, reminderRepository.countByComplaintIdAndAcknowledgedFalse(complaint.getId()));

        // Verify action_history logged with officer details
        List<ActionHistory> histories = actionHistoryRepository.findAllByComplaintIdOrderByCreatedAtAsc(complaint.getId());
        assertFalse(histories.isEmpty());
        ActionHistory latest = histories.get(histories.size() - 1);
        assertEquals("Inspector Rajesh", latest.getActorName());
        assertEquals("OFFICER", latest.getActorRole());
        assertEquals("FIR registered and CCTV footage retrieved from nearby junction.", latest.getRemarks());
        assertFalse(latest.isInternal(), "Public timeline flag was true, so isInternal should be false");

        // Verify citizen was notified
        assertEquals(1, notificationRepository.countByUserIdAndReadFalse(citizenUser.getId()));
    }

    @Test
    @DisplayName("Officer Update: Resolving grievance clears escalation flags and resets escalationLevel to 0")
    void testOfficerUpdate_ClearsEscalationOnResolution() {
        Complaint complaint = createAssignedComplaint("RSK-2026-UPDATE-2");
        complaint.setStatus(ComplaintStatus.ESCALATED);
        complaint.setEscalationLevel(1);
        complaintRepository.save(complaint);

        // Seed an open escalation
        Escalation openEsc = Escalation.builder()
                .complaint(complaint)
                .fromLevel(0)
                .toLevel(1)
                .triggerReason("Statutory breach")
                .acknowledged(false)
                .build();
        escalationRepository.save(openEsc);

        ComplaintDto.ProgressUpdateRequest request = new ComplaintDto.ProgressUpdateRequest(
                ComplaintStatus.RESOLVED,
                "Stolen vehicle recovered and returned to citizen.",
                true,
                null
        );

        ComplaintDto.OfficerDetailResponse response = complaintService.updateProgress(complaint.getId(), request, "officer1");

        assertEquals(ComplaintStatus.RESOLVED, response.getStatus());
        assertEquals(0, response.getEscalationLevel(), "Escalation level must be reset to 0 on resolution");
        assertEquals(100, response.getHealthScore(), "Resolved complaint health score must be 100");

        // Verify escalation marked acknowledged
        Escalation updatedEsc = escalationRepository.findById(openEsc.getId()).get();
        assertTrue(updatedEsc.isAcknowledged());
    }

    @Test
    @DisplayName("Security: canUpdateComplaint allows assigned officer, supervisor, and admin; rejects other officers and citizens")
    void testComplaintSecurity_canUpdateComplaint() {
        Complaint complaint = createAssignedComplaint("RSK-2026-SEC-1");

        // 1. Assigned officer (Rajesh, officerId=21) -> ALLOWED
        CustomUserDetails officer1Details = new CustomUserDetails(officerUser1.getId(), officerUser1.getUsername(), "pass", officerUser1.getFullName(), Role.OFFICER, 1L, 21L, true);
        Authentication authOfficer1 = new UsernamePasswordAuthenticationToken(officer1Details, null, officer1Details.getAuthorities());
        assertTrue(complaintSecurityService.canUpdateComplaint(authOfficer1, complaint.getId()));

        // 2. Department Supervisor (Ramesh, deptId=1) -> ALLOWED
        CustomUserDetails supervisorDetails = new CustomUserDetails(supervisorUser.getId(), supervisorUser.getUsername(), "pass", supervisorUser.getFullName(), Role.SUPERVISOR, 1L, null, true);
        Authentication authSupervisor = new UsernamePasswordAuthenticationToken(supervisorDetails, null, supervisorDetails.getAuthorities());
        assertTrue(complaintSecurityService.canUpdateComplaint(authSupervisor, complaint.getId()));

        // 3. Admin -> ALLOWED
        CustomUserDetails adminDetails = new CustomUserDetails(adminUser.getId(), adminUser.getUsername(), "pass", adminUser.getFullName(), Role.ADMIN, null, null, true);
        Authentication authAdmin = new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities());
        assertTrue(complaintSecurityService.canUpdateComplaint(authAdmin, complaint.getId()));

        // 4. Other Officer (Suresh, officerId=22) -> DENIED
        CustomUserDetails officer2Details = new CustomUserDetails(officerUser2.getId(), officerUser2.getUsername(), "pass", officerUser2.getFullName(), Role.OFFICER, 1L, 22L, true);
        Authentication authOfficer2 = new UsernamePasswordAuthenticationToken(officer2Details, null, officer2Details.getAuthorities());
        assertFalse(complaintSecurityService.canUpdateComplaint(authOfficer2, complaint.getId()));

        // 5. Citizen -> DENIED
        CustomUserDetails citizenDetails = new CustomUserDetails(citizenUser.getId(), citizenUser.getUsername(), "pass", citizenUser.getFullName(), Role.CITIZEN, null, null, true);
        Authentication authCitizen = new UsernamePasswordAuthenticationToken(citizenDetails, null, citizenDetails.getAuthorities());
        assertFalse(complaintSecurityService.canUpdateComplaint(authCitizen, complaint.getId()));
    }

    @Test
    @DisplayName("History: Citizens only see public updates without officer names; staff see internal notes")
    void testHistory_CitizenVsStaff() {
        Complaint complaint = createAssignedComplaint("RSK-2026-HIST-1");

        // Log one public action
        actionHistoryService.logAction(complaint.getId(), officerUser1.getId(), officerUser1.getFullName(), "OFFICER", "REGISTERED", null, "ASSIGNED", "Grievance accepted", false);
        // Log one internal confidential action
        actionHistoryService.logAction(complaint.getId(), officerUser1.getId(), officerUser1.getFullName(), "OFFICER", "INTERNAL_NOTE", "ASSIGNED", "ASSIGNED", "Confidential source lead verified", true);

        // Citizen view
        List<ActionHistoryDto.CitizenView> citizenHistory = complaintService.getCitizenHistory(complaint.getId());
        assertEquals(1, citizenHistory.size(), "Citizens must only see non-internal history rows");
        assertEquals("Grievance accepted", citizenHistory.get(0).getRemarks());

        // Internal staff view
        List<ActionHistoryDto.InternalView> internalHistory = complaintService.getInternalHistory(complaint.getId());
        assertEquals(2, internalHistory.size(), "Staff view must contain complete audit history including internal notes");
        assertTrue(internalHistory.stream().anyMatch(h -> "Confidential source lead verified".equals(h.getRemarks())));
    }

    @Test
    @DisplayName("Notification: Lifecycle verification (create, unread count, read single, read all)")
    void testNotificationLifecycle() {
        // Send 2 notifications to supervisor
        notificationService.sendInAppNotification(supervisorUser, null, "Alert 1", "Escalation warning");
        notificationService.sendInAppNotification(supervisorUser, null, "Alert 2", "Breach reminder");

        assertEquals(2, notificationService.getUnreadCountForUser(supervisorUser.getId()));

        List<NotificationDto.Response> list = notificationService.getNotificationsForUser(supervisorUser.getId());
        assertEquals(2, list.size());
        assertFalse(list.get(0).isRead());

        // Read first notification
        notificationService.markNotificationAsRead(list.get(0).getId(), supervisorUser.getId());
        assertEquals(1, notificationService.getUnreadCountForUser(supervisorUser.getId()));

        // Mark all as read
        notificationService.markAllAsRead(supervisorUser.getId());
        assertEquals(0, notificationService.getUnreadCountForUser(supervisorUser.getId()));
    }

    @Test
    @DisplayName("Reminder Acknowledge: Officer manually acknowledges reminder")
    void testReminderAcknowledge() {
        Complaint complaint = createAssignedComplaint("RSK-2026-REM-ACK");
        Reminder reminder = reminderRepository.save(Reminder.builder()
                .complaint(complaint)
                .officer(officer1)
                .reminderType("PRE_DEADLINE_NUDGE")
                .message("Reminder message")
                .acknowledged(false)
                .build());

        assertFalse(reminder.isAcknowledged());

        // Acknowledge reminder
        reminder.setAcknowledged(true);
        reminder.setAcknowledgedAt(LocalDateTime.now(clock));
        reminderRepository.save(reminder);

        Reminder refreshed = reminderRepository.findById(reminder.getId()).get();
        assertTrue(refreshed.isAcknowledged());
        assertNotNull(refreshed.getAcknowledgedAt());
    }

    // =========================================================================
    // In-memory Repositories
    // =========================================================================

    private static class InMemoryComplaintRepository implements ComplaintRepository {
        private final Map<Long, Complaint> byId = new HashMap<>();
        private long idGen = 1;

        @Override public synchronized <S extends Complaint> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            byId.put(entity.getId(), entity);
            return entity;
        }

        @Override public Optional<Complaint> findById(Long id) { return Optional.ofNullable(byId.get(id)); }
        @Override public List<Complaint> findAll() { return new ArrayList<>(byId.values()); }
        @Override public Optional<Complaint> findByTrackingId(String trackingId) { return byId.values().stream().filter(c -> trackingId.equals(c.getTrackingId())).findFirst(); }
        @Override public Optional<Complaint> findByTrackingNumber(String trackingNumber) { return byId.values().stream().filter(c -> trackingNumber.equals(c.getTrackingNumber())).findFirst(); }
        @Override public Optional<Complaint> findByTrackingIdOrTrackingNumber(String trackingId, String trackingNumber) { return findByTrackingId(trackingId); }
        @Override public List<Complaint> findAllByCitizenIdOrderByCreatedAtDesc(Long citizenId) { return Collections.emptyList(); }
        @Override public List<Complaint> findAllByDepartmentIdOrderByCreatedAtDesc(Long departmentId) { return Collections.emptyList(); }
        @Override public List<Complaint> findAllByAssignedOfficerIdOrderByCreatedAtDesc(Long officerId) { return Collections.emptyList(); }
        @Override public List<Complaint> findAllByStatus(ComplaintStatus status) { return Collections.emptyList(); }
        @Override public List<Complaint> findAllByDepartmentIdAndStatus(Long departmentId, ComplaintStatus status) { return Collections.emptyList(); }
        @Override public List<Complaint> findComplaintsNearingDeadline(LocalDateTime threshold, LocalDateTime now) { return Collections.emptyList(); }
        @Override public List<Complaint> findOverdueForEscalation(LocalDateTime now, int maxLevel) { return Collections.emptyList(); }
        @Override public long countByStatus(ComplaintStatus status) { return 0; }
        @Override public long countByDepartmentId(Long departmentId) { return 0; }
        @Override public long countByAssignedOfficerId(Long assignedOfficerId) { return 0; }
        @Override public org.springframework.data.domain.Page<Complaint> searchComplaints(Long deptId, Long officerId, Long citizenId, ComplaintStatus status, Priority priority, LocalDateTime startDate, LocalDateTime endDate, String search, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<Object[]> countByStatusAll() { return Collections.emptyList(); }
        @Override public List<Object[]> countByStatusForDepartment(Long deptId) { return Collections.emptyList(); }
        @Override public List<Object[]> countByStatusForOfficer(Long officerId) { return Collections.emptyList(); }
        @Override public List<Object[]> countByStatusForCitizen(Long citizenId) { return Collections.emptyList(); }
        @Override public List<Complaint> findOpenByDepartmentAndCategorySince(Long deptId, String category, LocalDateTime since) { return Collections.emptyList(); }
        @Override public List<Complaint> findAllByCommunityIssueIdOrderByCreatedAtAsc(Long communityIssueId) { return Collections.emptyList(); }
        @Override public List<Complaint> findAwaitingResolutionReviewBefore(LocalDateTime cutoff) { return Collections.emptyList(); }
        @Override public void flush() {}
        @Override public <S extends Complaint> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Complaint> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<Complaint> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Complaint getOne(Long aLong) { return null; }
        @Override public Complaint getById(Long aLong) { return null; }
        @Override public Complaint getReferenceById(Long aLong) { return null; }
        @Override public <S extends Complaint> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Complaint> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Complaint> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Complaint> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends Complaint> long count(Example<S> example) { return 0; }
        @Override public <S extends Complaint> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Complaint, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Complaint> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<Complaint> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return byId.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Complaint entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Complaint> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Complaint> findAll(Sort sort) { return new ArrayList<>(byId.values()); }
        @Override public Page<Complaint> findAll(Pageable pageable) { return null; }
    }

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Long, User> byId = new HashMap<>();
        private long idGen = 100;

        @Override public synchronized <S extends User> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            byId.put(entity.getId(), entity);
            return entity;
        }

        @Override public List<User> findAll() { return new ArrayList<>(byId.values()); }
        @Override public Optional<User> findById(Long id) { return Optional.ofNullable(byId.get(id)); }
        @Override public Optional<User> findByUsername(String username) {
            return byId.values().stream().filter(u -> username.equals(u.getUsername())).findFirst();
        }
        @Override public Optional<User> findByEmail(String email) {
            return byId.values().stream().filter(u -> email.equals(u.getEmail())).findFirst();
        }
        @Override public boolean existsByUsername(String username) { return findByUsername(username).isPresent(); }
        @Override public boolean existsByEmail(String email) { return findByEmail(email).isPresent(); }
        @Override public void flush() {}
        @Override public <S extends User> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<User> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public User getOne(Long aLong) { return null; }
        @Override public User getById(Long aLong) { return null; }
        @Override public User getReferenceById(Long aLong) { return null; }
        @Override public <S extends User> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends User> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends User> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends User> long count(Example<S> example) { return 0; }
        @Override public <S extends User> boolean exists(Example<S> example) { return false; }
        @Override public <S extends User, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<User> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return byId.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(User entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends User> entities) {}
        @Override public void deleteAll() {}
        @Override public List<User> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<User> findAll(Pageable pageable) { return null; }
    }

    private static class InMemoryDepartmentRepository implements DepartmentRepository {
        private final Map<Long, Department> byId = new HashMap<>();
        @Override public Optional<Department> findByCode(String code) { return Optional.empty(); }
        @Override public List<Department> findAllByActiveTrue() { return new ArrayList<>(byId.values()); }
        @Override public Optional<Department> findById(Long id) { return Optional.ofNullable(byId.get(id)); }
        @Override public <S extends Department> S save(S entity) { byId.put(entity.getId(), entity); return entity; }
        @Override public void flush() {}
        @Override public <S extends Department> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Department> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<Department> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Department getOne(Long aLong) { return null; }
        @Override public Department getById(Long aLong) { return null; }
        @Override public Department getReferenceById(Long aLong) { return null; }
        @Override public <S extends Department> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Department> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Department> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Department> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends Department> long count(Example<S> example) { return 0; }
        @Override public <S extends Department> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Department, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Department> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<Department> findAll() { return new ArrayList<>(byId.values()); }
        @Override public List<Department> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return byId.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Department entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Department> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Department> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<Department> findAll(Pageable pageable) { return null; }
    }

    private static class InMemoryOfficerRepository implements OfficerRepository {
        private final Map<Long, Officer> byId = new HashMap<>();
        @Override public Optional<Officer> findById(Long id) { return Optional.ofNullable(byId.get(id)); }
        @Override public Optional<Officer> findByUserId(Long userId) {
            return byId.values().stream().filter(o -> o.getUser().getId().equals(userId)).findFirst();
        }
        @Override public Optional<Officer> findByEmployeeCode(String employeeCode) { return Optional.empty(); }
        @Override public List<Officer> findAllByDepartmentId(Long departmentId) { return Collections.emptyList(); }
        @Override public <S extends Officer> S save(S entity) { byId.put(entity.getId(), entity); return entity; }
        @Override public void flush() {}
        @Override public <S extends Officer> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Officer> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<Officer> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Officer getOne(Long aLong) { return null; }
        @Override public Officer getById(Long aLong) { return null; }
        @Override public Officer getReferenceById(Long aLong) { return null; }
        @Override public <S extends Officer> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Officer> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Officer> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Officer> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends Officer> long count(Example<S> example) { return 0; }
        @Override public <S extends Officer> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Officer, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Officer> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<Officer> findAll() { return new ArrayList<>(byId.values()); }
        @Override public List<Officer> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return byId.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Officer entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Officer> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Officer> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<Officer> findAll(Pageable pageable) { return null; }
    }

    private static class InMemoryActionHistoryRepository implements ActionHistoryRepository {
        private final List<ActionHistory> list = new ArrayList<>();
        private long idGen = 1;

        @Override public synchronized <S extends ActionHistory> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            list.add(entity);
            return entity;
        }

        @Override public List<ActionHistory> findAllByComplaintIdOrderByCreatedAtAsc(Long complaintId) {
            return list.stream().filter(h -> h.getComplaintId().equals(complaintId)).toList();
        }
        @Override public List<ActionHistory> findAllByComplaintIdAndIsInternalFalseOrderByCreatedAtAsc(Long complaintId) {
            return list.stream().filter(h -> h.getComplaintId().equals(complaintId) && !h.isInternal()).toList();
        }
        @Override public void flush() {}
        @Override public <S extends ActionHistory> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends ActionHistory> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<ActionHistory> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public ActionHistory getOne(Long aLong) { return null; }
        @Override public ActionHistory getById(Long aLong) { return null; }
        @Override public ActionHistory getReferenceById(Long aLong) { return null; }
        @Override public <S extends ActionHistory> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends ActionHistory> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends ActionHistory> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends ActionHistory> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends ActionHistory> long count(Example<S> example) { return 0; }
        @Override public <S extends ActionHistory> boolean exists(Example<S> example) { return false; }
        @Override public <S extends ActionHistory, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends ActionHistory> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public Optional<ActionHistory> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<ActionHistory> findAll() { return new ArrayList<>(list); }
        @Override public List<ActionHistory> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return list.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(ActionHistory entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends ActionHistory> entities) {}
        @Override public void deleteAll() {}
        @Override public List<ActionHistory> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<ActionHistory> findAll(Pageable pageable) { return null; }
    }

    private static class InMemoryReminderRepository implements ReminderRepository {
        private final List<Reminder> list = new ArrayList<>();
        private long idGen = 1;

        @Override public synchronized <S extends Reminder> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            list.removeIf(r -> r.getId().equals(entity.getId()));
            list.add(entity);
            return entity;
        }

        @Override public Optional<Reminder> findById(Long id) { return list.stream().filter(r -> r.getId().equals(id)).findFirst(); }
        @Override public List<Reminder> findAllByComplaintIdAndAcknowledgedFalse(Long complaintId) {
            return list.stream().filter(r -> r.getComplaint() != null && r.getComplaint().getId().equals(complaintId) && !r.isAcknowledged()).toList();
        }
        @Override public long countByComplaintIdAndAcknowledgedFalse(Long complaintId) {
            return list.stream().filter(r -> r.getComplaint() != null && r.getComplaint().getId().equals(complaintId) && !r.isAcknowledged()).count();
        }
        @Override public List<Reminder> findAllByOfficerIdOrderBySentAtDesc(Long officerId) { return Collections.emptyList(); }
        @Override public List<Reminder> findAllByComplaintIdOrderBySentAtDesc(Long complaintId) { return Collections.emptyList(); }
        @Override public boolean existsByComplaintIdAndReminderTypeAndAcknowledgedFalse(Long complaintId, String reminderType) { return false; }
        @Override public boolean existsByComplaintIdAndReminderTypeAndSentAtAfter(Long complaintId, String reminderType, LocalDateTime after) { return false; }
        @Override public long countByOfficerIdAndAcknowledgedFalse(Long officerId) { return 0; }
        @Override public Optional<Reminder> findTopByComplaintIdAndReminderTypeOrderBySentAtDesc(Long complaintId, String reminderType) { return Optional.empty(); }
        @Override public void flush() {}
        @Override public <S extends Reminder> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Reminder> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<Reminder> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Reminder getOne(Long aLong) { return null; }
        @Override public Reminder getById(Long aLong) { return null; }
        @Override public Reminder getReferenceById(Long aLong) { return null; }
        @Override public <S extends Reminder> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Reminder> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Reminder> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Reminder> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends Reminder> long count(Example<S> example) { return 0; }
        @Override public <S extends Reminder> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Reminder, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Reminder> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<Reminder> findAll() { return new ArrayList<>(list); }
        @Override public List<Reminder> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return list.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Reminder entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Reminder> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Reminder> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<Reminder> findAll(Pageable pageable) { return null; }
    }

    private static class InMemoryEscalationRepository implements EscalationRepository {
        private final List<Escalation> list = new ArrayList<>();
        private long idGen = 1;

        @Override public synchronized <S extends Escalation> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            list.removeIf(e -> e.getId().equals(entity.getId()));
            list.add(entity);
            return entity;
        }

        @Override public Optional<Escalation> findById(Long id) { return list.stream().filter(e -> e.getId().equals(id)).findFirst(); }
        @Override public List<Escalation> findAllByComplaintIdAndAcknowledgedFalse(Long complaintId) {
            return list.stream().filter(e -> e.getComplaint() != null && e.getComplaint().getId().equals(complaintId) && !e.isAcknowledged()).toList();
        }
        @Override public List<Escalation> findAllByComplaintIdOrderByEscalatedAtDesc(Long complaintId) { return Collections.emptyList(); }
        @Override public List<Escalation> findAllByNotifiedSupervisorIdOrderByEscalatedAtDesc(Long supervisorId) { return Collections.emptyList(); }
        @Override public boolean existsByComplaintIdAndToLevel(Long complaintId, Integer toLevel) { return false; }
        @Override public long countByAcknowledgedFalse() { return 0; }
        @Override public void flush() {}
        @Override public <S extends Escalation> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Escalation> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<Escalation> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Escalation getOne(Long aLong) { return null; }
        @Override public Escalation getById(Long aLong) { return null; }
        @Override public Escalation getReferenceById(Long aLong) { return null; }
        @Override public <S extends Escalation> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Escalation> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Escalation> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Escalation> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends Escalation> long count(Example<S> example) { return 0; }
        @Override public <S extends Escalation> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Escalation, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Escalation> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<Escalation> findAll() { return new ArrayList<>(list); }
        @Override public List<Escalation> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return list.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Escalation entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Escalation> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Escalation> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<Escalation> findAll(Pageable pageable) { return null; }
    }

    private static class InMemoryNotificationRepository implements NotificationRepository {
        private final List<Notification> list = new ArrayList<>();
        private long idGen = 1;

        @Override public synchronized <S extends Notification> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            list.removeIf(n -> n.getId().equals(entity.getId()));
            list.add(entity);
            return entity;
        }

        @Override public List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId) {
            return list.stream().filter(n -> n.getUser().getId().equals(userId)).toList();
        }
        @Override public long countByUserIdAndReadFalse(Long userId) {
            return list.stream().filter(n -> n.getUser().getId().equals(userId) && !n.isRead()).count();
        }
        @Override public Optional<Notification> findByIdAndUserId(Long id, Long userId) {
            return list.stream().filter(n -> n.getId().equals(id) && n.getUser().getId().equals(userId)).findFirst();
        }
        @Override public int markAllAsReadForUser(Long userId, LocalDateTime now) {
            int c = 0;
            for (Notification n : list) {
                if (n.getUser().getId().equals(userId) && !n.isRead()) {
                    n.setRead(true);
                    n.setReadAt(now);
                    c++;
                }
            }
            return c;
        }
        @Override public void flush() {}
        @Override public <S extends Notification> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Notification> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<Notification> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Notification getOne(Long aLong) { return null; }
        @Override public Notification getById(Long aLong) { return null; }
        @Override public Notification getReferenceById(Long aLong) { return null; }
        @Override public <S extends Notification> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Notification> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Notification> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Notification> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends Notification> long count(Example<S> example) { return 0; }
        @Override public <S extends Notification> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Notification, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Notification> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public Optional<Notification> findById(Long aLong) { return list.stream().filter(n -> n.getId().equals(aLong)).findFirst(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<Notification> findAll() { return new ArrayList<>(list); }
        @Override public List<Notification> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return list.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Notification entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Notification> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Notification> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<Notification> findAll(Pageable pageable) { return null; }
    }

    private static class InMemorySystemSettingRepository implements SystemSettingRepository {
        private final Map<String, SystemSetting> map = new HashMap<>();

        @Override public Optional<SystemSetting> findBySettingKey(String settingKey) { return Optional.ofNullable(map.get(settingKey)); }
        @Override public boolean existsBySettingKey(String settingKey) { return map.containsKey(settingKey); }
        @Override public <S extends SystemSetting> S save(S entity) { map.put(entity.getSettingKey(), entity); return entity; }
        @Override public void flush() {}
        @Override public <S extends SystemSetting> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends SystemSetting> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<SystemSetting> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public SystemSetting getOne(Long aLong) { return null; }
        @Override public SystemSetting getById(Long aLong) { return null; }
        @Override public SystemSetting getReferenceById(Long aLong) { return null; }
        @Override public <S extends SystemSetting> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends SystemSetting> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends SystemSetting> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends SystemSetting> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends SystemSetting> long count(Example<S> example) { return 0; }
        @Override public <S extends SystemSetting> boolean exists(Example<S> example) { return false; }
        @Override public <S extends SystemSetting, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends SystemSetting> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public Optional<SystemSetting> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<SystemSetting> findAll() { return new ArrayList<>(map.values()); }
        @Override public List<SystemSetting> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return map.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(SystemSetting entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends SystemSetting> entities) {}
        @Override public void deleteAll() {}
        @Override public List<SystemSetting> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<SystemSetting> findAll(Pageable pageable) { return null; }
    }

    private static class InMemoryTrackingSequenceRepository implements TrackingSequenceRepository {
        private final Map<Integer, TrackingSequence> map = new HashMap<>();
        @Override public Optional<TrackingSequence> findByYearForUpdate(int year) { return Optional.ofNullable(map.get(year)); }
        @Override public Optional<TrackingSequence> findByYear(int year) { return Optional.ofNullable(map.get(year)); }
        @Override public synchronized <S extends TrackingSequence> S saveAndFlush(S entity) { map.put(entity.getYear(), entity); return entity; }
        @Override public synchronized <S extends TrackingSequence> S save(S entity) { return saveAndFlush(entity); }
        @Override public void flush() {}
        @Override public <S extends TrackingSequence> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<TrackingSequence> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public TrackingSequence getOne(Long aLong) { return null; }
        @Override public TrackingSequence getById(Long aLong) { return null; }
        @Override public TrackingSequence getReferenceById(Long aLong) { return null; }
        @Override public <S extends TrackingSequence> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends TrackingSequence> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends TrackingSequence> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends TrackingSequence> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends TrackingSequence> long count(Example<S> example) { return 0; }
        @Override public <S extends TrackingSequence> boolean exists(Example<S> example) { return false; }
        @Override public <S extends TrackingSequence, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends TrackingSequence> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public Optional<TrackingSequence> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<TrackingSequence> findAll() { return new ArrayList<>(map.values()); }
        @Override public List<TrackingSequence> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return map.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(TrackingSequence entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends TrackingSequence> entities) {}
        @Override public void deleteAll() {}
        @Override public List<TrackingSequence> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<TrackingSequence> findAll(Pageable pageable) { return null; }
    }
}

