package com.resolvetrack.reminder;

import com.resolvetrack.auth.Role;
import com.resolvetrack.auth.User;
import com.resolvetrack.auth.UserRepository;
import com.resolvetrack.common.SystemSetting;
import com.resolvetrack.common.SystemSettingRepository;
import com.resolvetrack.complaint.Complaint;
import com.resolvetrack.complaint.ComplaintRepository;
import com.resolvetrack.complaint.ComplaintStatus;
import com.resolvetrack.complaint.Priority;
import com.resolvetrack.department.Department;
import com.resolvetrack.escalation.Escalation;
import com.resolvetrack.escalation.EscalationRepository;
import com.resolvetrack.escalation.EscalationService;
import com.resolvetrack.history.ActionHistory;
import com.resolvetrack.history.ActionHistoryRepository;
import com.resolvetrack.history.ActionHistoryService;
import com.resolvetrack.notification.Notification;
import com.resolvetrack.notification.NotificationRepository;
import com.resolvetrack.notification.NotificationService;
import com.resolvetrack.officer.Officer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class FollowUpEngineTest {

    private MutableClock clock;
    private InMemoryComplaintRepository complaintRepository;
    private InMemoryReminderRepository reminderRepository;
    private InMemoryEscalationRepository escalationRepository;
    private InMemoryActionHistoryRepository actionHistoryRepository;
    private InMemorySystemSettingRepository systemSettingRepository;
    private InMemoryUserRepository userRepository;
    private InMemoryNotificationRepository notificationRepository;

    private NotificationService notificationService;
    private ActionHistoryService actionHistoryService;
    private HealthScoreService healthScoreService;
    private ReminderService reminderService;
    private EscalationService escalationService;
    private FollowUpScheduler followUpScheduler;

    private Officer assignedOfficer;
    private User supervisorUser;
    private User adminUser;
    private Department department;

    private static final Instant T0 = Instant.parse("2026-11-01T10:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    @BeforeEach
    void setUp() {
        clock = new MutableClock(T0, ZONE);

        complaintRepository = new InMemoryComplaintRepository();
        reminderRepository = new InMemoryReminderRepository();
        escalationRepository = new InMemoryEscalationRepository();
        actionHistoryRepository = new InMemoryActionHistoryRepository();
        systemSettingRepository = new InMemorySystemSettingRepository();
        userRepository = new InMemoryUserRepository();
        notificationRepository = new InMemoryNotificationRepository();

        notificationService = new NotificationService(notificationRepository, clock);
        actionHistoryService = new ActionHistoryService(actionHistoryRepository);
        healthScoreService = new HealthScoreService(complaintRepository, reminderRepository, actionHistoryRepository, clock);

        reminderService = new ReminderService(
                reminderRepository,
                complaintRepository,
                actionHistoryService,
                systemSettingRepository,
                notificationService,
                clock
        );

        escalationService = new EscalationService(
                escalationRepository,
                complaintRepository,
                reminderRepository,
                actionHistoryService,
                systemSettingRepository,
                userRepository,
                notificationService,
                clock
        );

        followUpScheduler = new FollowUpScheduler(
                reminderService,
                escalationService,
                healthScoreService,
                systemSettingRepository,
                clock
        );

        // Seed settings
        systemSettingRepository.save(new SystemSetting("demo.mode", "false", "Demo flag"));
        systemSettingRepository.save(new SystemSetting("reminder.lead_time", "24", "Reminder lead hours"));
        systemSettingRepository.save(new SystemSetting("escalation.delay", "48", "Escalation Tier 1 delay"));
        systemSettingRepository.save(new SystemSetting("escalation.level2.delay", "96", "Escalation Tier 2 delay"));
        systemSettingRepository.save(new SystemSetting("reminder.ignored.threshold", "2", "Ignored reminders threshold"));

        // Seed department & users
        department = new Department(1L, "POL", "Police Department", "ಪೊಲೀಸ್ ಇಲಾಖೆ", "Law & Order", 7, 24, true);

        User officerUser = User.builder().id(101L).username("officer_rajesh").fullName("Inspector Rajesh").role(Role.OFFICER).departmentId(1L).build();
        supervisorUser = User.builder().id(102L).username("sup_ramesh").fullName("SP Ramesh").role(Role.SUPERVISOR).departmentId(1L).build();
        adminUser = User.builder().id(103L).username("admin").fullName("System Administrator").role(Role.ADMIN).build();

        userRepository.save(officerUser);
        userRepository.save(supervisorUser);
        userRepository.save(adminUser);

        assignedOfficer = Officer.builder().id(201L).user(officerUser).department(department).employeeCode("EMP-POL-001").designation("Inspector").build();
    }

    private Complaint createTestComplaint(String trackingId, LocalDateTime deadline, ComplaintStatus status) {
        Complaint c = Complaint.builder()
                .trackingId(trackingId)
                .trackingNumber(trackingId)
                .title("Theft report " + trackingId)
                .description("Citizen lost property")
                .category("theft")
                .status(status)
                .department(department)
                .assignedOfficer(assignedOfficer)
                .followUpDeadline(deadline)
                .slaDueDate(deadline)
                .escalationLevel(0)
                .build();
        return complaintRepository.save(c);
    }

    @Test
    @DisplayName("Rule 1: Reminder before deadline (nudge dispatched within lead time window)")
    void testReminderBeforeDeadline() {
        LocalDateTime now = LocalDateTime.now(clock);
        // Deadline is 48 hours away
        LocalDateTime deadline = now.plusHours(48);
        Complaint complaint = createTestComplaint("RSK-2026-001", deadline, ComplaintStatus.ASSIGNED);

        // At T0: 48 hours before deadline -> outside 24h lead time -> no reminder
        followUpScheduler.runCycle();
        assertEquals(0, reminderRepository.count());

        // Advance clock by 25 hours -> now deadline is 23 hours away (within 24h lead time)
        clock.advance(Duration.ofHours(25));
        FollowUpScheduler.CycleResult result = followUpScheduler.runCycle();

        assertEquals(1, result.getRemindersDispatched(), "Expected 1 pre-deadline reminder nudge");
        assertEquals(1, reminderRepository.count());

        Reminder reminder = reminderRepository.findAll().get(0);
        assertEquals("PRE_DEADLINE_NUDGE", reminder.getReminderType());
        assertFalse(reminder.isAcknowledged());
        assertEquals(complaint.getId(), reminder.getComplaint().getId());

        // Verify action_history row with SYSTEM actor
        List<ActionHistory> histories = actionHistoryRepository.findAllByComplaintIdOrderByCreatedAtAsc(complaint.getId());
        assertEquals(1, histories.size());
        assertEquals("SYSTEM", histories.get(0).getActorRole());
        assertEquals("SYSTEM", histories.get(0).getActorName());
        assertEquals("REMINDER_DISPATCHED", histories.get(0).getAction());
        assertTrue(histories.get(0).isInternal());
    }

    @Test
    @DisplayName("Rule 2: No duplicate reminders (idempotency under identical cycle/deadline)")
    void testNoDuplicateReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime deadline = now.plusHours(20); // within 24h lead time
        Complaint complaint = createTestComplaint("RSK-2026-002", deadline, ComplaintStatus.ASSIGNED);

        // First run -> creates 1 reminder
        followUpScheduler.runCycle();
        assertEquals(1, reminderRepository.count());

        // Advance 1 hour -> still within lead time, but reminder already issued and unacknowledged
        clock.advance(Duration.ofHours(1));
        FollowUpScheduler.CycleResult result = followUpScheduler.runCycle();

        assertEquals(0, result.getRemindersDispatched(), "Should not create duplicate reminder");
        assertEquals(1, reminderRepository.count(), "Total reminders should remain 1");
    }

    @Test
    @DisplayName("Rule 3: Overdue transition (complaint past deadline transitions to OVERDUE)")
    void testOverdueTransition() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime deadline = now.plusHours(10);
        Complaint complaint = createTestComplaint("RSK-2026-003", deadline, ComplaintStatus.ASSIGNED);

        // Before deadline -> status remains ASSIGNED
        followUpScheduler.runCycle();
        assertEquals(ComplaintStatus.ASSIGNED, complaintRepository.findById(complaint.getId()).get().getStatus());

        // Advance clock past deadline (by 11 hours)
        clock.advance(Duration.ofHours(11));
        FollowUpScheduler.CycleResult result = followUpScheduler.runCycle();

        assertEquals(1, result.getOverdueTransitioned(), "Expected 1 complaint transitioned to OVERDUE");
        Complaint updated = complaintRepository.findById(complaint.getId()).get();
        assertEquals(ComplaintStatus.OVERDUE, updated.getStatus());

        // Verify action_history logged with actor SYSTEM
        List<ActionHistory> histories = actionHistoryRepository.findAllByComplaintIdOrderByCreatedAtAsc(complaint.getId());
        Optional<ActionHistory> overdueHistory = histories.stream()
                .filter(h -> "OVERDUE_TRANSITION".equals(h.getAction()))
                .findFirst();
        assertTrue(overdueHistory.isPresent());
        assertEquals("SYSTEM", overdueHistory.get().getActorRole());
        assertEquals("OVERDUE", overdueHistory.get().getToStatus());
    }

    @Test
    @DisplayName("Rule 4: Escalation after delay (statutory breach triggers Level 1 escalation)")
    void testEscalationAfterDelay() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime deadline = now.plusHours(10);
        Complaint complaint = createTestComplaint("RSK-2026-004", deadline, ComplaintStatus.ASSIGNED);

        // Advance past deadline (10h) + past escalation delay (48h) = 59 hours
        clock.advance(Duration.ofHours(59));
        FollowUpScheduler.CycleResult result = followUpScheduler.runCycle();

        assertEquals(1, result.getEscalationsProcessed(), "Expected Level 1 escalation");

        Complaint updated = complaintRepository.findById(complaint.getId()).get();
        assertEquals(1, updated.getEscalationLevel());
        assertEquals(ComplaintStatus.ESCALATED, updated.getStatus());

        // Verify Escalation entity created
        List<Escalation> escalations = escalationRepository.findAll();
        assertEquals(1, escalations.size());
        assertEquals(0, escalations.get(0).getFromLevel());
        assertEquals(1, escalations.get(0).getToLevel());
        assertEquals(supervisorUser.getId(), escalations.get(0).getNotifiedSupervisorId());

        // Verify action_history logged with actor SYSTEM
        List<ActionHistory> histories = actionHistoryRepository.findAllByComplaintIdOrderByCreatedAtAsc(complaint.getId());
        Optional<ActionHistory> escHistory = histories.stream()
                .filter(h -> "ESCALATED".equals(h.getAction()))
                .findFirst();
        assertTrue(escHistory.isPresent());
        assertEquals("SYSTEM", escHistory.get().getActorRole());
    }

    @Test
    @DisplayName("Rule 5: Escalation after ignored reminders (multiple reminders past deadline trigger early escalation)")
    void testEscalationAfterIgnoredReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime deadline = now.plusHours(10);
        Complaint complaint = createTestComplaint("RSK-2026-005", deadline, ComplaintStatus.ASSIGNED);

        // Advance 12 hours -> past deadline, but NOT yet past 48h escalation delay
        clock.advance(Duration.ofHours(12));

        // Simulate 2 unacknowledged reminders (threshold is 2)
        reminderRepository.save(Reminder.builder().complaint(complaint).officer(assignedOfficer).reminderType("PRE_DEADLINE_NUDGE").acknowledged(false).build());
        reminderRepository.save(Reminder.builder().complaint(complaint).officer(assignedOfficer).reminderType("OVERDUE_WARNING").acknowledged(false).build());

        FollowUpScheduler.CycleResult result = followUpScheduler.runCycle();

        assertEquals(1, result.getEscalationsProcessed(), "Should escalate to Level 1 due to ignored reminders");
        Complaint updated = complaintRepository.findById(complaint.getId()).get();
        assertEquals(1, updated.getEscalationLevel());
        assertEquals(ComplaintStatus.ESCALATED, updated.getStatus());

        List<Escalation> escalations = escalationRepository.findAll();
        assertEquals(1, escalations.size());
        assertTrue(escalations.get(0).getTriggerReason().contains("ignored reminder nudges"));
    }

    @Test
    @DisplayName("Rule 6: Level-2 escalation (breach past tier-2 threshold elevates to District Admin)")
    void testLevel2Escalation() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime deadline = now.plusHours(10);
        Complaint complaint = createTestComplaint("RSK-2026-006", deadline, ComplaintStatus.ASSIGNED);

        // First advance 60 hours -> Level 1 escalation occurs
        clock.advance(Duration.ofHours(60));
        followUpScheduler.runCycle();
        assertEquals(1, complaintRepository.findById(complaint.getId()).get().getEscalationLevel());

        // Advance further past Tier-2 delay (96 hours past deadline or 48h after L1)
        clock.advance(Duration.ofHours(50));
        FollowUpScheduler.CycleResult result = followUpScheduler.runCycle();

        assertEquals(1, result.getEscalationsProcessed(), "Should process Level 2 escalation");
        Complaint updated = complaintRepository.findById(complaint.getId()).get();
        assertEquals(2, updated.getEscalationLevel(), "Complaint should now be at Level 2");

        List<Escalation> escalations = escalationRepository.findAll();
        assertEquals(2, escalations.size());
        Escalation l2 = escalations.stream().filter(e -> e.getToLevel() == 2).findFirst().orElseThrow();
        assertEquals(1, l2.getFromLevel());
        assertEquals(2, l2.getToLevel());
        assertEquals(adminUser.getId(), l2.getNotifiedSupervisorId(), "Level 2 must notify District Admin");
    }

    @Test
    @DisplayName("Rule 7: No action on resolved complaints (resolved grievances are never nudged or escalated)")
    void testNoActionOnResolvedComplaints() {
        LocalDateTime now = LocalDateTime.now(clock);
        // Past deadline
        LocalDateTime deadline = now.minusHours(50);

        Complaint resolvedComplaint = createTestComplaint("RSK-2026-007A", deadline, ComplaintStatus.RESOLVED);
        Complaint rejectedComplaint = createTestComplaint("RSK-2026-007B", deadline, ComplaintStatus.REJECTED);

        FollowUpScheduler.CycleResult result = followUpScheduler.runCycle();

        assertEquals(0, result.getRemindersDispatched());
        assertEquals(0, result.getOverdueTransitioned());
        assertEquals(0, result.getEscalationsProcessed());

        assertEquals(ComplaintStatus.RESOLVED, complaintRepository.findById(resolvedComplaint.getId()).get().getStatus());
        assertEquals(ComplaintStatus.REJECTED, complaintRepository.findById(rejectedComplaint.getId()).get().getStatus());
        assertEquals(0, reminderRepository.count());
        assertEquals(0, escalationRepository.count());
    }

    @Test
    @DisplayName("Demo Mode: Interval units treated as minutes instead of hours")
    void testDemoModeUnitsTreatedAsMinutes() {
        systemSettingRepository.save(new SystemSetting("demo.mode", "true", "Demo active"));
        systemSettingRepository.save(new SystemSetting("reminder.lead_time", "10", "10 minutes lead time"));
        systemSettingRepository.save(new SystemSetting("escalation.delay", "20", "20 minutes delay"));

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime deadline = now.plusMinutes(15);
        Complaint complaint = createTestComplaint("RSK-2026-DEMO", deadline, ComplaintStatus.ASSIGNED);

        // At T0: 15 minutes to deadline -> outside 10 min lead time -> no reminder
        followUpScheduler.runCycle();
        assertEquals(0, reminderRepository.count());

        // Advance 6 minutes -> now 9 minutes to deadline (within 10 min lead time)
        clock.advance(Duration.ofMinutes(6));
        FollowUpScheduler.CycleResult result1 = followUpScheduler.runCycle();
        assertEquals(1, result1.getRemindersDispatched());

        // Advance 10 minutes -> past deadline -> overdue transition
        clock.advance(Duration.ofMinutes(10));
        FollowUpScheduler.CycleResult result2 = followUpScheduler.runCycle();
        assertEquals(1, result2.getOverdueTransitioned());
        assertEquals(ComplaintStatus.OVERDUE, complaintRepository.findById(complaint.getId()).get().getStatus());

        // Advance 21 minutes -> 20 minutes past deadline in demo mode -> Level 1 escalation!
        clock.advance(Duration.ofMinutes(21));
        FollowUpScheduler.CycleResult result3 = followUpScheduler.runCycle();
        assertEquals(1, result3.getEscalationsProcessed());
        assertEquals(1, complaintRepository.findById(complaint.getId()).get().getEscalationLevel());
    }

    // =========================================================================
    // Test In-Memory Clocks & Repositories
    // =========================================================================

    private static class MutableClock extends Clock {
        private Instant currentInstant;
        private final ZoneId zone;

        public MutableClock(Instant initialInstant, ZoneId zone) {
            this.currentInstant = initialInstant;
            this.zone = zone;
        }

        public void advance(Duration duration) {
            this.currentInstant = this.currentInstant.plus(duration);
        }

        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return new MutableClock(currentInstant, zone); }
        @Override public Instant instant() { return currentInstant; }
    }

    private static class InMemoryComplaintRepository implements ComplaintRepository {
        private final Map<Long, Complaint> byId = new HashMap<>();
        private long idGen = 1;

        @Override
        public synchronized <S extends Complaint> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            byId.put(entity.getId(), entity);
            return entity;
        }

        @Override public Optional<Complaint> findById(Long id) { return Optional.ofNullable(byId.get(id)); }
        @Override public List<Complaint> findAll() { return new ArrayList<>(byId.values()); }
        @Override public Optional<Complaint> findByTrackingId(String trackingId) {
            return byId.values().stream().filter(c -> trackingId.equals(c.getTrackingId())).findFirst();
        }
        @Override public Optional<Complaint> findByTrackingNumber(String trackingNumber) {
            return byId.values().stream().filter(c -> trackingNumber.equals(c.getTrackingNumber())).findFirst();
        }
        @Override public Optional<Complaint> findByTrackingIdOrTrackingNumber(String trackingId, String trackingNumber) {
            return findByTrackingId(trackingId);
        }
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

    private static class InMemoryReminderRepository implements ReminderRepository {
        private final List<Reminder> list = new ArrayList<>();
        private long idGen = 1;

        @Override
        public synchronized <S extends Reminder> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            list.removeIf(r -> r.getId().equals(entity.getId()));
            list.add(entity);
            return entity;
        }

        @Override public List<Reminder> findAll() { return new ArrayList<>(list); }
        @Override public Optional<Reminder> findById(Long id) { return list.stream().filter(r -> r.getId().equals(id)).findFirst(); }
        @Override public List<Reminder> findAllByOfficerIdOrderBySentAtDesc(Long officerId) {
            return list.stream().filter(r -> r.getOfficer() != null && r.getOfficer().getId().equals(officerId)).toList();
        }
        @Override public List<Reminder> findAllByComplaintIdOrderBySentAtDesc(Long complaintId) {
            return list.stream().filter(r -> r.getComplaint() != null && r.getComplaint().getId().equals(complaintId)).toList();
        }
        @Override public List<Reminder> findAllByComplaintIdAndAcknowledgedFalse(Long complaintId) {
            return list.stream().filter(r -> r.getComplaint() != null && r.getComplaint().getId().equals(complaintId) && !r.isAcknowledged()).toList();
        }
        @Override public boolean existsByComplaintIdAndReminderTypeAndAcknowledgedFalse(Long complaintId, String reminderType) {
            return list.stream().anyMatch(r -> r.getComplaint() != null && r.getComplaint().getId().equals(complaintId)
                    && reminderType.equals(r.getReminderType()) && !r.isAcknowledged());
        }
        @Override public boolean existsByComplaintIdAndReminderTypeAndSentAtAfter(Long complaintId, String reminderType, LocalDateTime after) {
            return list.stream().anyMatch(r -> r.getComplaint() != null && r.getComplaint().getId().equals(complaintId)
                    && reminderType.equals(r.getReminderType()) && r.getSentAt() != null && r.getSentAt().isAfter(after));
        }
        @Override public long countByComplaintIdAndAcknowledgedFalse(Long complaintId) {
            return list.stream().filter(r -> r.getComplaint() != null && r.getComplaint().getId().equals(complaintId) && !r.isAcknowledged()).count();
        }
        @Override public long countByOfficerIdAndAcknowledgedFalse(Long officerId) {
            return list.stream().filter(r -> r.getOfficer() != null && r.getOfficer().getId().equals(officerId) && !r.isAcknowledged()).count();
        }
        @Override public Optional<Reminder> findTopByComplaintIdAndReminderTypeOrderBySentAtDesc(Long complaintId, String reminderType) {
            return list.stream().filter(r -> r.getComplaint() != null && r.getComplaint().getId().equals(complaintId)
                    && reminderType.equals(r.getReminderType())).max(Comparator.comparing(Reminder::getSentAt));
        }
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

        @Override
        public synchronized <S extends Escalation> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            list.removeIf(e -> e.getId().equals(entity.getId()));
            list.add(entity);
            return entity;
        }

        @Override public List<Escalation> findAll() { return new ArrayList<>(list); }
        @Override public Optional<Escalation> findById(Long id) { return list.stream().filter(e -> e.getId().equals(id)).findFirst(); }
        @Override public List<Escalation> findAllByComplaintIdOrderByEscalatedAtDesc(Long complaintId) {
            return list.stream().filter(e -> e.getComplaint() != null && e.getComplaint().getId().equals(complaintId))
                    .sorted((a, b) -> b.getEscalatedAt() != null && a.getEscalatedAt() != null ? b.getEscalatedAt().compareTo(a.getEscalatedAt()) : 0)
                    .toList();
        }
        @Override public List<Escalation> findAllByNotifiedSupervisorIdOrderByEscalatedAtDesc(Long supervisorId) {
            return list.stream().filter(e -> supervisorId.equals(e.getNotifiedSupervisorId())).toList();
        }
        @Override public List<Escalation> findAllByComplaintIdAndAcknowledgedFalse(Long complaintId) {
            return list.stream().filter(e -> e.getComplaint() != null && e.getComplaint().getId().equals(complaintId) && !e.isAcknowledged()).toList();
        }
        @Override public boolean existsByComplaintIdAndToLevel(Long complaintId, Integer toLevel) {
            return list.stream().anyMatch(e -> e.getComplaint() != null && e.getComplaint().getId().equals(complaintId) && toLevel.equals(e.getToLevel()));
        }
        @Override public long countByAcknowledgedFalse() {
            return list.stream().filter(e -> !e.isAcknowledged()).count();
        }
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

    private static class InMemoryActionHistoryRepository implements ActionHistoryRepository {
        private final List<ActionHistory> list = new ArrayList<>();
        private long idGen = 1;

        @Override
        public synchronized <S extends ActionHistory> S save(S entity) {
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

    private static class InMemorySystemSettingRepository implements SystemSettingRepository {
        private final Map<String, SystemSetting> map = new HashMap<>();

        @Override public Optional<SystemSetting> findBySettingKey(String settingKey) { return Optional.ofNullable(map.get(settingKey)); }
        @Override public boolean existsBySettingKey(String settingKey) { return map.containsKey(settingKey); }
        @Override public <S extends SystemSetting> S save(S entity) {
            map.put(entity.getSettingKey(), entity);
            return entity;
        }
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

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<Long, User> byId = new HashMap<>();
        private long idGen = 1000;

        @Override
        public synchronized <S extends User> S save(S entity) {
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

    private static class InMemoryNotificationRepository implements NotificationRepository {
        private final List<Notification> list = new ArrayList<>();
        private long idGen = 1;

        @Override
        public synchronized <S extends Notification> S save(S entity) {
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
}

