package com.resolvetrack.complaint;

import com.resolvetrack.auth.CustomUserDetails;
import com.resolvetrack.auth.Role;
import com.resolvetrack.auth.User;
import com.resolvetrack.department.Department;
import com.resolvetrack.officer.Officer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ComplaintSecurityServiceTest {

    private ComplaintSecurityService securityService;
    private InMemoryComplaintRepository complaintRepository;

    private Department policeDept;
    private Department pwdDept;
    private Officer policeOfficer1;
    private Officer policeOfficer2;
    private Complaint policeComplaint1;
    private Complaint pwdComplaint;

    @BeforeEach
    void setUp() {
        complaintRepository = new InMemoryComplaintRepository();
        securityService = new ComplaintSecurityService(complaintRepository);

        // Departments
        policeDept = Department.builder().id(1L).code("POL").nameEn("Police").build();
        pwdDept = Department.builder().id(3L).code("PWD").nameEn("Public Works").build();

        // Officers
        policeOfficer1 = Officer.builder().id(101L).department(policeDept).employeeCode("EMP-POL-001").build();
        policeOfficer2 = Officer.builder().id(102L).department(policeDept).employeeCode("EMP-POL-002").build();

        // Citizen
        User citizen = User.builder().id(50L).username("citizen1").role(Role.CITIZEN).build();

        // Complaints
        policeComplaint1 = Complaint.builder()
                .id(1001L)
                .trackingId("RT-2026-100001")
                .department(policeDept)
                .assignedOfficer(policeOfficer1)
                .citizen(citizen)
                .title("Traffic Signal Failure")
                .description("Signal blinking yellow")
                .category("Traffic")
                .status(ComplaintStatus.IN_PROGRESS)
                .build();
        complaintRepository.save(policeComplaint1);

        pwdComplaint = Complaint.builder()
                .id(1002L)
                .trackingId("RT-2026-100002")
                .department(pwdDept)
                .citizen(citizen)
                .title("Bridge Crack")
                .description("Structural crack on flyover")
                .category("Roads")
                .status(ComplaintStatus.SUBMITTED)
                .build();
        complaintRepository.save(pwdComplaint);
    }

    private Authentication createAuth(Long userId, String username, Role role, Long deptId, Long officerId) {
        CustomUserDetails details = new CustomUserDetails(
                userId,
                username,
                "secret",
                role,
                deptId,
                officerId,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()), new SimpleGrantedAuthority(role.name()))
        );
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    @Test
    @DisplayName("Admin has full access to any complaint across all departments")
    void testAdmin_HasAccessToAllComplaints() {
        Authentication adminAuth = createAuth(1L, "admin", Role.ADMIN, null, null);

        assertTrue(securityService.canAccessComplaint(adminAuth, 1001L));
        assertTrue(securityService.canAccessComplaint(adminAuth, 1002L));
        assertTrue(securityService.canAccessDepartment(adminAuth, 1L));
        assertTrue(securityService.canAccessDepartment(adminAuth, 3L));
    }

    @Test
    @DisplayName("Supervisor can access complaints belonging to their department only")
    void testSupervisor_CanAccessOwnDepartmentOnly() {
        Authentication policeSupervisorAuth = createAuth(2L, "supervisor_police", Role.SUPERVISOR, 1L, null);

        // Can access Police grievance (dept 1)
        assertTrue(securityService.canAccessComplaint(policeSupervisorAuth, 1001L));

        // CANNOT access PWD grievance (dept 3)
        assertFalse(securityService.canAccessComplaint(policeSupervisorAuth, 1002L));

        // Department access check
        assertTrue(securityService.canAccessDepartment(policeSupervisorAuth, 1L));
        assertFalse(securityService.canAccessDepartment(policeSupervisorAuth, 3L));
    }

    @Test
    @DisplayName("Officer can only access complaints assigned directly to them")
    void testOfficer_CanAccessOnlyAssignedComplaints() {
        Authentication officer1Auth = createAuth(4L, "officer_police", Role.OFFICER, 1L, 101L);
        Authentication officer2Auth = createAuth(5L, "officer_police_2", Role.OFFICER, 1L, 102L);

        // Officer 1 is assigned to complaint 1001 -> Allowed
        assertTrue(securityService.canAccessComplaint(officer1Auth, 1001L));

        // Officer 2 is in same department (Police) but NOT assigned to complaint 1001 -> Denied
        assertFalse(securityService.canAccessComplaint(officer2Auth, 1001L));

        // Officer 1 cannot access unassigned PWD complaint -> Denied
        assertFalse(securityService.canAccessComplaint(officer1Auth, 1002L));
    }

    @Test
    @DisplayName("Data Entry operator has access to lodge and process submissions")
    void testDataEntry_HasAccess() {
        Authentication dataEntryAuth = createAuth(9L, "dataentry_operator", Role.DATA_ENTRY, null, null);

        assertTrue(securityService.canAccessComplaint(dataEntryAuth, 1001L));
        assertTrue(securityService.canAccessComplaint(dataEntryAuth, 1002L));
    }

    @Test
    @DisplayName("Citizen can only access their own lodged complaints")
    void testCitizen_CanAccessOnlyOwnComplaints() {
        Authentication ownerCitizen = createAuth(50L, "citizen1", Role.CITIZEN, null, null);
        Authentication otherCitizen = createAuth(51L, "citizen2", Role.CITIZEN, null, null);

        assertTrue(securityService.canAccessComplaint(ownerCitizen, 1001L));
        assertFalse(securityService.canAccessComplaint(otherCitizen, 1001L));
    }

    // =========================================================================
    // In-memory ComplaintRepository
    // =========================================================================

    private static class InMemoryComplaintRepository implements ComplaintRepository {
        private final Map<Long, Complaint> store = new HashMap<>();

        @Override public <S extends Complaint> S save(S entity) {
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override public Optional<Complaint> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override public Optional<Complaint> findByTrackingId(String trackingId) { return Optional.empty(); }
        @Override public Optional<Complaint> findByTrackingNumber(String trackingNumber) { return Optional.empty(); }
        @Override public Optional<Complaint> findByTrackingIdOrTrackingNumber(String trackingId, String trackingNumber) { return Optional.empty(); }
        @Override public List<Complaint> findAllByCitizenIdOrderByCreatedAtDesc(Long citizenId) { return Collections.emptyList(); }
        @Override public List<Complaint> findAllByDepartmentIdOrderByCreatedAtDesc(Long departmentId) { return Collections.emptyList(); }
        @Override public List<Complaint> findAllByAssignedOfficerIdOrderByCreatedAtDesc(Long assignedOfficerId) { return Collections.emptyList(); }
        @Override public List<Complaint> findAllByStatus(ComplaintStatus status) { return Collections.emptyList(); }
        @Override public List<Complaint> findAllByDepartmentIdAndStatus(Long departmentId, ComplaintStatus status) { return Collections.emptyList(); }
        @Override public List<Complaint> findComplaintsNearingDeadline(java.time.LocalDateTime threshold, java.time.LocalDateTime now) { return Collections.emptyList(); }
        @Override public List<Complaint> findOverdueForEscalation(java.time.LocalDateTime now, int maxLevel) { return Collections.emptyList(); }
        @Override public long countByStatus(ComplaintStatus status) { return 0; }
        @Override public long countByDepartmentId(Long departmentId) { return 0; }
        @Override public long countByAssignedOfficerId(Long assignedOfficerId) { return 0; }
        @Override public org.springframework.data.domain.Page<Complaint> searchComplaints(Long deptId, Long officerId, Long citizenId, ComplaintStatus status, Priority priority, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, String search, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
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
        @Override public <S extends Complaint> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends Complaint> List<S> findAll(org.springframework.data.domain.Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Complaint> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Complaint> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends Complaint> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends Complaint> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends Complaint, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
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
        @Override public List<Complaint> findAll(org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public org.springframework.data.domain.Page<Complaint> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
    }
}

