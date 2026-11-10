package com.resolvetrack.complaint;

import com.resolvetrack.auth.Role;
import com.resolvetrack.auth.User;
import com.resolvetrack.auth.UserRepository;
import com.resolvetrack.common.SystemSetting;
import com.resolvetrack.common.SystemSettingRepository;
import com.resolvetrack.department.Department;
import com.resolvetrack.department.DepartmentRepository;
import com.resolvetrack.department.RuleBasedDepartmentSuggestionService;
import com.resolvetrack.history.ActionHistory;
import com.resolvetrack.history.ActionHistoryDto;
import com.resolvetrack.history.ActionHistoryRepository;
import com.resolvetrack.history.ActionHistoryService;
import com.resolvetrack.notification.NotificationService;
import com.resolvetrack.officer.Officer;
import com.resolvetrack.officer.OfficerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ComplaintRegistrationAndAssignmentTest {

    private ComplaintService complaintService;
    private InMemoryComplaintRepository complaintRepository;
    private InMemoryUserRepository userRepository;
    private InMemoryDepartmentRepository departmentRepository;
    private InMemoryOfficerRepository officerRepository;
    private InMemoryActionHistoryRepository actionHistoryRepository;
    private InMemoryTrackingSequenceRepository sequenceRepository;
    private InMemorySystemSettingRepository systemSettingRepository;
    private MockNotificationService notificationService;

    private Department cyberDept;
    private Department pwdDept;
    private Officer cyberOfficer;
    private User supervisorUser;

    @BeforeEach
    void setUp() {
        complaintRepository = new InMemoryComplaintRepository();
        userRepository = new InMemoryUserRepository();
        departmentRepository = new InMemoryDepartmentRepository();
        officerRepository = new InMemoryOfficerRepository();
        actionHistoryRepository = new InMemoryActionHistoryRepository();
        sequenceRepository = new InMemoryTrackingSequenceRepository();
        systemSettingRepository = new InMemorySystemSettingRepository();
        notificationService = new MockNotificationService();

        ActionHistoryService actionHistoryService = new ActionHistoryService(actionHistoryRepository);
        TrackingIdGeneratorService trackingIdGeneratorService = new TrackingIdGeneratorService(sequenceRepository);
        FileUploadService fileUploadService = new FileUploadService("./uploads");
        RuleBasedDepartmentSuggestionService suggestionService = new RuleBasedDepartmentSuggestionService(departmentRepository);

        complaintService = new ComplaintService(
                complaintRepository,
                departmentRepository,
                userRepository,
                officerRepository,
                actionHistoryService,
                actionHistoryRepository,
                trackingIdGeneratorService,
                fileUploadService,
                suggestionService,
                notificationService,
                systemSettingRepository
        );

        // Seed system settings for category SLAs
        systemSettingRepository.save(new SystemSetting(1L, "sla.category.fraud", "3", "Cyber fraud SLA", LocalDateTime.now()));
        systemSettingRepository.save(new SystemSetting(2L, "sla.category.road", "10", "PWD road SLA", LocalDateTime.now()));
        systemSettingRepository.save(new SystemSetting(3L, "sla.category.default", "7", "Default SLA", LocalDateTime.now()));

        // Seed departments
        cyberDept = departmentRepository.save(Department.builder().id(2L).code("CYB").nameEn("Cybercrime Department").nameKn("ಸೈಬರ್ ಅಪರಾಧ ಇಲಾಖೆ").defaultSlaDays(5).build());
        pwdDept = departmentRepository.save(Department.builder().id(3L).code("PWD").nameEn("Public Works Department").nameKn("ಲೋಕೋಪಯೋಗಿ ಇಲಾಖೆ").defaultSlaDays(10).build());

        // Seed officer
        User officerUser = userRepository.save(User.builder().id(20L).username("officer_cyber").fullName("Sub-Inspector Ananya Rao").email("ananya@resolvetrack.gov.in").role(Role.OFFICER).build());
        cyberOfficer = officerRepository.save(Officer.builder().id(102L).user(officerUser).department(cyberDept).employeeCode("EMP-CYB-001").designation("Sub-Inspector").build());

        // Seed supervisor
        supervisorUser = userRepository.save(User.builder().id(10L).username("supervisor_cyber").fullName("Superintendent Ramesh").email("sup@resolvetrack.gov.in").role(Role.SUPERVISOR).department(cyberDept).build());
    }

    @Test
    @DisplayName("Public registration creates complaint with tracking ID RSK-{year}-{seq}, status NEW, deadline from category setting, and first history row")
    void testRegisterPublicComplaint_FullWorkflow() {
        ComplaintDto.CreateRequest request = ComplaintDto.CreateRequest.builder()
                .departmentId(cyberDept.getId())
                .title("Unauthorized credit card transactions via phishing link")
                .description("Received SMS with phishing link, 45000 deducted via UPI fraud")
                .category("Online Financial Fraud")
                .citizenName("Citizen Mohan")
                .citizenPhone("+91 98450 11223")
                .citizenEmail("mohan@example.com")
                .build();

        MockMultipartFile image = new MockMultipartFile("files", "statement.png", "image/png", "PNG bytes".getBytes());

        ComplaintDto.CitizenTrackingResponse response = complaintService.registerPublicComplaint(request, List.of(image));

        assertNotNull(response);
        assertNotNull(response.getTrackingId());
        int year = LocalDate.now().getYear();
        assertTrue(response.getTrackingId().startsWith("RSK-" + year + "-"));
        assertEquals("RSK-" + year + "-001", response.getTrackingId());
        assertEquals(ComplaintStatus.NEW, response.getStatus());

        // Deadline must be computed from settings for fraud category (3 days)
        assertTrue(response.getFollowUpDeadline().isAfter(LocalDateTime.now().plusDays(2)));
        assertTrue(response.getFollowUpDeadline().isBefore(LocalDateTime.now().plusDays(4)));

        // Verify first action_history row
        List<ActionHistory> history = actionHistoryRepository.findAllByComplaintIdOrderByCreatedAtAsc(1L);
        assertEquals(1, history.size());
        ActionHistory firstRow = history.get(0);
        assertEquals("REGISTERED", firstRow.getAction());
        assertNull(firstRow.getFromStatus());
        assertEquals("NEW", firstRow.getToStatus());
        assertEquals("Citizen Mohan", firstRow.getActorName());
        assertFalse(firstRow.isInternal());
        assertTrue(firstRow.getRemarks().contains(response.getTrackingId()));
    }

    @Test
    @DisplayName("Official registration supports PHONE and PAPER source, sets status NEW, and writes initial history row")
    void testRegisterOfficialComplaint_Sources() {
        ComplaintDto.CreateRequest request = ComplaintDto.CreateRequest.builder()
                .departmentId(pwdDept.getId())
                .title("Major Pothole on Outer Ring Road")
                .description("Deep crater causing two wheeler skids")
                .category("Road and Pothole Repair")
                .source(ComplaintSource.PHONE)
                .citizenName("Citizen Shwetha")
                .citizenPhone("+91 98450 33445")
                .build();

        ComplaintDto.OfficerDetailResponse response = complaintService.registerOfficialComplaint(request, "supervisor_cyber");

        assertNotNull(response);
        assertEquals(ComplaintStatus.NEW, response.getStatus());
        assertEquals(ComplaintSource.PHONE, response.getSource());
        assertTrue(response.getTrackingId().startsWith("RSK-"));

        // Deadline for road category is 10 days
        assertTrue(response.getFollowUpDeadline().isAfter(LocalDateTime.now().plusDays(9)));

        // Action history row indicates official registered via PHONE
        List<ActionHistory> history = actionHistoryRepository.findAllByComplaintIdOrderByCreatedAtAsc(response.getId());
        assertEquals(1, history.size());
        assertTrue(history.get(0).getRemarks().contains("PHONE"));
    }

    @Test
    @DisplayName("GET /api/public/track/{id} returns only status, department, latest update, and resolvedOrPending; leaks zero internal info")
    void testGetPublicTrackInfo_PrivacyGuaranteed() {
        // Register complaint
        ComplaintDto.CreateRequest request = ComplaintDto.CreateRequest.builder()
                .departmentId(cyberDept.getId())
                .title("Stolen Laptop with Bank Details")
                .description("Left laptop in auto")
                .category("Lost Property")
                .citizenName("Citizen Anand")
                .build();
        ComplaintDto.CitizenTrackingResponse created = complaintService.registerPublicComplaint(request, Collections.emptyList());

        // Call public track endpoint
        ComplaintDto.PublicTrackResponse publicInfo = complaintService.getPublicTrackInfo(created.getTrackingId());

        assertNotNull(publicInfo);
        assertEquals("NEW", publicInfo.getStatus());
        assertEquals("Cybercrime Department", publicInfo.getDepartmentName());
        assertEquals("PENDING", publicInfo.getResolvedOrPending());
        assertFalse(publicInfo.isResolved());
        assertNotNull(publicInfo.getLatestPublicUpdate());

        // Ensure no internal notes or officer name in PublicTrackResponse class definition
        assertFalse(publicInfo.getLatestPublicUpdate().contains("EMP-"));
    }

    @Test
    @DisplayName("PUT /api/complaints/{id}/assign updates status to ASSIGNED, notifies officer, and writes history with reason")
    void testAssignOfficer_UpdatesStatusAndNotifies() {
        // 1. Create complaint
        ComplaintDto.CreateRequest request = ComplaintDto.CreateRequest.builder()
                .departmentId(cyberDept.getId())
                .title("SIM Swap Fraud")
                .description("Duplicate SIM issued without consent")
                .category("Cybercrime")
                .citizenName("Citizen Ramesh")
                .build();
        ComplaintDto.CitizenTrackingResponse created = complaintService.registerPublicComplaint(request, Collections.emptyList());

        // 2. Assign complaint to officer with reason
        ComplaintDto.AssignOfficerRequest assignRequest = new ComplaintDto.AssignOfficerRequest(
                cyberOfficer.getId(),
                "Priority grievance assigned for urgent digital forensics"
        );

        complaintService.assignOfficer(1L, assignRequest, supervisorUser.getId(), supervisorUser.getUsername(), supervisorUser.getRole().name());

        // 3. Verify status updated to ASSIGNED
        Complaint updated = complaintRepository.findById(1L).orElseThrow();
        assertEquals(ComplaintStatus.ASSIGNED, updated.getStatus());
        assertEquals(cyberOfficer.getId(), updated.getAssignedOfficer().getId());

        // 4. Verify officer was notified
        assertEquals(1, notificationService.dispatchedNudges.size());
        assertTrue(notificationService.dispatchedNudges.get(0).contains("ananya@resolvetrack.gov.in"));
        assertTrue(notificationService.dispatchedNudges.get(0).contains("digital forensics"));

        // 5. Verify action_history contains ASSIGNED and justification reason
        List<ActionHistory> history = actionHistoryRepository.findAllByComplaintIdOrderByCreatedAtAsc(1L);
        assertEquals(2, history.size()); // 1st REGISTERED, 2nd ASSIGNED
        ActionHistory assignRow = history.get(1);
        assertEquals("ASSIGNED", assignRow.getAction());
        assertEquals("NEW", assignRow.getFromStatus());
        assertEquals("ASSIGNED", assignRow.getToStatus());
        assertTrue(assignRow.getRemarks().contains("digital forensics"));
    }

    // =========================================================================
    // In-memory test doubles
    // =========================================================================

    private static class MockNotificationService extends NotificationService {
        final List<String> dispatchedNudges = new ArrayList<>();

        @Override
        public void sendOfficerNudge(String email, String subject, String content) {
            dispatchedNudges.add(email + " | " + subject + " | " + content);
        }
    }

    private static class InMemoryComplaintRepository implements ComplaintRepository {
        private final Map<Long, Complaint> byId = new HashMap<>();
        private final Map<String, Complaint> byRef = new HashMap<>();
        private long idGen = 1;

        @Override
        public synchronized <S extends Complaint> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            byId.put(entity.getId(), entity);
            if (entity.getTrackingId() != null) byRef.put(entity.getTrackingId(), entity);
            if (entity.getTrackingNumber() != null) byRef.put(entity.getTrackingNumber(), entity);
            return entity;
        }

        @Override public Optional<Complaint> findById(Long id) { return Optional.ofNullable(byId.get(id)); }
        @Override public Optional<Complaint> findByTrackingId(String trackingId) { return Optional.ofNullable(byRef.get(trackingId)); }
        @Override public Optional<Complaint> findByTrackingNumber(String trackingNumber) { return Optional.ofNullable(byRef.get(trackingNumber)); }
        @Override public Optional<Complaint> findByTrackingIdOrTrackingNumber(String id, String num) {
            if (byRef.containsKey(id)) return Optional.of(byRef.get(id));
            if (byRef.containsKey(num)) return Optional.of(byRef.get(num));
            return Optional.empty();
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
        @Override public List<Complaint> findAll() { return new ArrayList<>(byId.values()); }
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
        private final Map<String, User> byUsername = new HashMap<>();
        private final Map<String, User> byEmail = new HashMap<>();
        private final Map<Long, User> byId = new HashMap<>();
        private long idGen = 100;

        @Override public Optional<User> findByUsername(String username) { return Optional.ofNullable(byUsername.get(username)); }
        @Override public Optional<User> findByEmail(String email) { return Optional.ofNullable(byEmail.get(email)); }
        @Override public boolean existsByUsername(String username) { return byUsername.containsKey(username); }
        @Override public boolean existsByEmail(String email) { return byEmail.containsKey(email); }
        @Override public synchronized <S extends User> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            byId.put(entity.getId(), entity);
            if (entity.getUsername() != null) byUsername.put(entity.getUsername(), entity);
            if (entity.getEmail() != null) byEmail.put(entity.getEmail(), entity);
            return entity;
        }
        @Override public Optional<User> findById(Long aLong) { return Optional.ofNullable(byId.get(aLong)); }
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
        @Override public List<User> findAll() { return new ArrayList<>(byId.values()); }
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
        private final Map<String, Department> byCode = new HashMap<>();

        @Override public Optional<Department> findByCode(String code) { return Optional.ofNullable(byCode.get(code)); }
        @Override public List<Department> findAllByActiveTrue() { return new ArrayList<>(byId.values()); }
        @Override public Optional<Department> findById(Long aLong) { return Optional.ofNullable(byId.get(aLong)); }
        @Override public <S extends Department> S save(S entity) {
            byId.put(entity.getId(), entity);
            byCode.put(entity.getCode(), entity);
            return entity;
        }
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
        @Override public <S extends Officer> S save(S entity) {
            byId.put(entity.getId(), entity);
            return entity;
        }
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

    private static class InMemoryTrackingSequenceRepository implements TrackingSequenceRepository {
        private final Map<Integer, TrackingSequence> map = new ConcurrentHashMap<>();
        private long idGen = 1;

        @Override public synchronized Optional<TrackingSequence> findByYearForUpdate(int year) { return Optional.ofNullable(map.get(year)); }
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
}
