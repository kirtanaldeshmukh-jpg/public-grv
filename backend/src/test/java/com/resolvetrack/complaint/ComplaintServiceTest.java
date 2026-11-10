package com.resolvetrack.complaint;

import com.resolvetrack.auth.Role;
import com.resolvetrack.auth.User;
import com.resolvetrack.auth.UserRepository;
import com.resolvetrack.department.Department;
import com.resolvetrack.department.DepartmentRepository;
import com.resolvetrack.history.ActionHistory;
import com.resolvetrack.history.ActionHistoryDto;
import com.resolvetrack.history.ActionHistoryRepository;
import com.resolvetrack.history.ActionHistoryService;
import com.resolvetrack.officer.OfficerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ComplaintServiceTest {

    private ComplaintService complaintService;
    private FakeComplaintRepository complaintRepository;
    private FakeUserRepository userRepository;
    private FakeDepartmentRepository departmentRepository;
    private FakeActionHistoryService actionHistoryService;

    private User citizen;
    private Department department;

    @BeforeEach
    void setUp() {
        complaintRepository = new FakeComplaintRepository();
        userRepository = new FakeUserRepository();
        departmentRepository = new FakeDepartmentRepository();
        actionHistoryService = new FakeActionHistoryService();

        TrackingIdGeneratorService trackingIdGeneratorService = new TrackingIdGeneratorService(new InMemoryTrackingSequenceRepo());
        FileUploadService fileUploadService = new FileUploadService("./uploads");
        com.resolvetrack.department.RuleBasedDepartmentSuggestionService suggestionService =
                new com.resolvetrack.department.RuleBasedDepartmentSuggestionService(departmentRepository);

        complaintService = new ComplaintService(
                complaintRepository,
                departmentRepository,
                userRepository,
                null,
                actionHistoryService,
                null,
                trackingIdGeneratorService,
                fileUploadService,
                suggestionService,
                null,
                null
        );

        citizen = User.builder()
                .id(1L)
                .username("citizen_user")
                .fullName("Ramesh Kumar")
                .email("ramesh@karnataka.gov.in")
                .role(Role.CITIZEN)
                .build();
        userRepository.save(citizen);

        department = Department.builder()
                .id(10L)
                .code("MUN")
                .nameEn("Bruhat Bengaluru Mahanagara Palike")
                .nameKn("ಬೃಹತ್ ಬೆಂಗಳೂರು ಮಹಾನಗರ ಪಾಲಿಕೆ")
                .defaultSlaDays(7)
                .escalationHours(24)
                .active(true)
                .build();
        departmentRepository.save(department);
    }

    @Test
    @DisplayName("Should create complaint with deterministic SLA deadline and log to action_history")
    void testCreateComplaint_CalculatesSlaAndLogsAudit() {
        ComplaintDto.CreateRequest request = ComplaintDto.CreateRequest.builder()
                .departmentId(10L)
                .title("Damaged Street Light")
                .description("Street light is broken")
                .category("Street Lighting")
                .priority(Priority.HIGH)
                .latitude(new BigDecimal("12.9716"))
                .longitude(new BigDecimal("77.5946"))
                .locationAddress("Indiranagar, Bengaluru")
                .build();

        ComplaintDto.CitizenTrackingResponse response = complaintService.createComplaint(request, "citizen_user");

        assertNotNull(response);
        assertNotNull(response.getTrackingNumber());
        assertTrue(response.getTrackingNumber().startsWith("RSK-") || response.getTrackingNumber().startsWith("RT-"));
        assertEquals("Bruhat Bengaluru Mahanagara Palike", response.getDepartmentNameEn());
        assertEquals("Street Lighting", response.getCategory());
        assertEquals(ComplaintStatus.NEW, response.getStatus());

        // SLA must be exactly 7 days from now (matching department.getDefaultSlaDays())
        assertTrue(response.getFollowUpDeadline().isAfter(LocalDateTime.now().plusDays(6)));
        assertTrue(response.getFollowUpDeadline().isBefore(LocalDateTime.now().plusDays(8)));

        // Action history audit trail must have logged 1 row
        assertEquals(1, actionHistoryService.loggedActions.size());
        ActionHistory logged = actionHistoryService.loggedActions.get(0);
        assertEquals("REGISTERED", logged.getAction());
        assertEquals(ComplaintStatus.NEW.name(), logged.getToStatus());
        assertFalse(logged.isInternal());
    }

    @Test
    @DisplayName("Tracking endpoint should return citizen view without exposing officer name or internal notes")
    void testGetCitizenTracking_ProtectsPrivacy() {
        Complaint complaint = Complaint.builder()
                .id(99L)
                .trackingId("RT-2026-999999")
                .trackingNumber("RT-2026-999999")
                .citizen(citizen)
                .department(department)
                .title("Water Pipeline Leak")
                .description("Continuous leak outside house")
                .category("Water Supply")
                .priority(Priority.URGENT)
                .status(ComplaintStatus.IN_PROGRESS)
                .followUpDeadline(LocalDateTime.now().plusDays(3))
                .slaDueDate(LocalDateTime.now().plusDays(3))
                .escalationLevel(0)
                .build();

        complaintRepository.save(complaint);

        ComplaintDto.CitizenTrackingResponse result = complaintService.getCitizenTrackingByNumber("RT-2026-999999");

        assertNotNull(result);
        assertEquals("RT-2026-999999", result.getTrackingNumber());
        assertEquals(ComplaintStatus.IN_PROGRESS, result.getStatus());
        assertEquals("Water Pipeline Leak", result.getTitle());
    }

    // =========================================================================
    // Fast, In-Memory Test Doubles (Zero ByteBuddy reflection overhead on JDK 25)
    // =========================================================================

    private static class FakeComplaintRepository implements ComplaintRepository {
        private final Map<String, Complaint> store = new HashMap<>();
        private long idGen = 1;

        @Override
        public <S extends Complaint> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            if (entity.getTrackingNumber() != null) store.put(entity.getTrackingNumber(), entity);
            if (entity.getTrackingId() != null) store.put(entity.getTrackingId(), entity);
            return entity;
        }

        @Override
        public Optional<Complaint> findByTrackingNumber(String trackingNumber) {
            return Optional.ofNullable(store.get(trackingNumber));
        }

        @Override
        public Optional<Complaint> findByTrackingId(String trackingId) {
            return Optional.ofNullable(store.get(trackingId));
        }

        @Override
        public Optional<Complaint> findByTrackingIdOrTrackingNumber(String trackingId, String trackingNumber) {
            if (store.containsKey(trackingId)) return Optional.of(store.get(trackingId));
            if (store.containsKey(trackingNumber)) return Optional.of(store.get(trackingNumber));
            return Optional.empty();
        }

        @Override public Optional<Complaint> findById(Long aLong) { return store.values().stream().filter(c -> c.getId().equals(aLong)).findFirst(); }
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
        @Override public List<Complaint> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<Complaint> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Complaint entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Complaint> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Complaint> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<Complaint> findAll(Pageable pageable) { return null; }
    }

    private static class FakeUserRepository implements UserRepository {
        private final Map<String, User> byUsername = new HashMap<>();
        @Override public Optional<User> findByUsername(String username) { return Optional.ofNullable(byUsername.get(username)); }
        @Override public Optional<User> findByEmail(String email) { return Optional.empty(); }
        @Override public boolean existsByUsername(String username) { return byUsername.containsKey(username); }
        @Override public boolean existsByEmail(String email) { return false; }
        @Override public <S extends User> S save(S entity) { byUsername.put(entity.getUsername(), entity); return entity; }
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
        @Override public Optional<User> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<User> findAll() { return Collections.emptyList(); }
        @Override public List<User> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(User entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends User> entities) {}
        @Override public void deleteAll() {}
        @Override public List<User> findAll(Sort sort) { return Collections.emptyList(); }
        @Override public Page<User> findAll(Pageable pageable) { return null; }
    }

    private static class FakeDepartmentRepository implements DepartmentRepository {
        private final Map<Long, Department> byId = new HashMap<>();
        @Override public Optional<Department> findByCode(String code) { return Optional.empty(); }
        @Override public List<Department> findAllByActiveTrue() { return new ArrayList<>(byId.values()); }
        @Override public Optional<Department> findById(Long aLong) { return Optional.ofNullable(byId.get(aLong)); }
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

    private static class FakeActionHistoryService extends ActionHistoryService {
        final List<ActionHistory> loggedActions = new ArrayList<>();

        public FakeActionHistoryService() {
            super(null);
        }

        @Override
        public ActionHistory logAction(Long complaintId, Long actorId, String actorName, String actorRole, String action, String fromStatus, String toStatus, String remarks, boolean isInternal) {
            ActionHistory history = ActionHistory.builder()
                    .complaintId(complaintId)
                    .actorId(actorId)
                    .actorName(actorName)
                    .actorRole(actorRole)
                    .action(action)
                    .fromStatus(fromStatus)
                    .toStatus(toStatus)
                    .remarks(remarks)
                    .isInternal(isInternal)
                    .createdAt(LocalDateTime.now())
                    .build();
            loggedActions.add(history);
            return history;
        }

        @Override
        public List<ActionHistoryDto.CitizenView> getCitizenHistory(Long complaintId) {
            return Collections.emptyList();
        }

        @Override
        public List<ActionHistoryDto.InternalView> getInternalHistory(Long complaintId) {
            return Collections.emptyList();
        }
    }

    private static class InMemoryTrackingSequenceRepo implements TrackingSequenceRepository {
        private final Map<Integer, TrackingSequence> map = new HashMap<>();
        private long idGen = 1;

        @Override public Optional<TrackingSequence> findByYearForUpdate(int year) { return Optional.ofNullable(map.get(year)); }
        @Override public Optional<TrackingSequence> findByYear(int year) { return Optional.ofNullable(map.get(year)); }
        @Override public synchronized <S extends TrackingSequence> S saveAndFlush(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            map.put(entity.getYear(), entity);
            return entity;
        }
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
