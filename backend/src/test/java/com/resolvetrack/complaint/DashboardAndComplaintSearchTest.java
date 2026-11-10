package com.resolvetrack.complaint;

import com.resolvetrack.analytics.DashboardController;
import com.resolvetrack.analytics.DashboardService;
import com.resolvetrack.analytics.DashboardSummaryDto;
import com.resolvetrack.auth.CustomUserDetails;
import com.resolvetrack.auth.Role;
import com.resolvetrack.auth.User;
import com.resolvetrack.common.ApiResponse;
import com.resolvetrack.common.PageResponse;
import com.resolvetrack.department.Department;
import com.resolvetrack.officer.Officer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class DashboardAndComplaintSearchTest {

    private InMemoryComplaintRepository complaintRepository;
    private DashboardService dashboardService;
    private DashboardController dashboardController;
    private ComplaintService complaintService;
    private ComplaintController complaintController;

    private Department deptPolice;
    private Department deptPbbm;
    private Officer officerRajesh;
    private Officer officerSuresh;
    private User citizenAnand;
    private User citizenPriya;

    private CustomUserDetails adminUserDetails;
    private CustomUserDetails supervisorUserDetails;
    private CustomUserDetails officerUserDetails;
    private CustomUserDetails citizenUserDetails;

    @BeforeEach
    void setUp() {
        complaintRepository = new InMemoryComplaintRepository();
        dashboardService = new DashboardService(complaintRepository);
        dashboardController = new DashboardController(dashboardService);

        // Minimal complaint service setup for searching
        complaintService = new ComplaintService(
                complaintRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        complaintController = new ComplaintController(complaintService, null, null);

        // Departments
        deptPolice = Department.builder().id(1L).code("POL").nameEn("Police Department").nameKn("ಪೊಲೀಸ್ ಇಲಾಖೆ").build();
        deptPbbm = Department.builder().id(2L).code("BBMP").nameEn("Bruhat Bengaluru Mahanagara Palike").nameKn("ಬಿಬಿಎಂಪಿ").build();

        // Users & Officers
        User userRajesh = User.builder().id(11L).username("rajesh").fullName("Rajesh Kumar").role(Role.OFFICER).department(deptPolice).build();
        officerRajesh = Officer.builder().id(101L).user(userRajesh).department(deptPolice).employeeCode("POL-001").build();

        User userSuresh = User.builder().id(12L).username("suresh").fullName("Suresh Gowda").role(Role.OFFICER).department(deptPbbm).build();
        officerSuresh = Officer.builder().id(102L).user(userSuresh).department(deptPbbm).employeeCode("BBMP-001").build();

        User userSupervisor = User.builder().id(20L).username("sup_police").fullName("SP Police").role(Role.SUPERVISOR).department(deptPolice).build();

        citizenAnand = User.builder().id(201L).username("anand").fullName("Anand Sharma").phoneNumber("+91 98888 11111").role(Role.CITIZEN).build();
        citizenPriya = User.builder().id(202L).username("priya").fullName("Priya Patel").phoneNumber("+91 98888 22222").role(Role.CITIZEN).build();

        // UserDetails for auth simulation
        adminUserDetails = new CustomUserDetails(1L, "admin", "pass", "District Administrator", Role.ADMIN, null, null, true);
        supervisorUserDetails = new CustomUserDetails(userSupervisor.getId(), userSupervisor.getUsername(), "pass", userSupervisor.getFullName(), Role.SUPERVISOR, deptPolice.getId(), null, true);
        officerUserDetails = new CustomUserDetails(userRajesh.getId(), userRajesh.getUsername(), "pass", userRajesh.getFullName(), Role.OFFICER, deptPolice.getId(), officerRajesh.getId(), true);
        citizenUserDetails = new CustomUserDetails(citizenAnand.getId(), citizenAnand.getUsername(), "pass", citizenAnand.getFullName(), Role.CITIZEN, null, null, true);

        // Seed 6 diverse complaints
        // 1. Dept Police, Officer Rajesh, Citizen Anand, Status NEW, Priority HIGH, HealthScore 95
        complaintRepository.save(createComplaint(1L, "RSK-2026-001", "Theft of two-wheeler", "Hero Splendor stolen near bus stop", "Theft", deptPolice, officerRajesh, citizenAnand, ComplaintStatus.NEW, Priority.HIGH, 95, LocalDateTime.of(2026, 11, 1, 10, 0)));

        // 2. Dept Police, Officer Rajesh, Citizen Anand, Status IN_PROGRESS, Priority MEDIUM, HealthScore 80
        complaintRepository.save(createComplaint(2L, "RSK-2026-002", "Cyber scam through UPI link", "Lost 50000 rupees in lottery scam", "Cybercrime", deptPolice, officerRajesh, citizenAnand, ComplaintStatus.IN_PROGRESS, Priority.MEDIUM, 80, LocalDateTime.of(2026, 11, 2, 11, 30)));

        // 3. Dept Police, Officer Rajesh, Citizen Priya, Status OVERDUE, Priority URGENT, HealthScore 35
        complaintRepository.save(createComplaint(3L, "RSK-2026-003", "Assault case delay", "No FIR registered after 3 days", "Assault", deptPolice, officerRajesh, citizenPriya, ComplaintStatus.OVERDUE, Priority.URGENT, 35, LocalDateTime.of(2026, 11, 3, 9, 0)));

        // 4. Dept Police, Unassigned, Citizen Priya, Status ASSIGNED, Priority LOW, HealthScore 90
        complaintRepository.save(createComplaint(4L, "RSK-2026-004", "Noise complaint at night", "Loudspeakers after 10 PM", "Public Nuisance", deptPolice, null, citizenPriya, ComplaintStatus.ASSIGNED, Priority.LOW, 90, LocalDateTime.of(2026, 11, 4, 22, 0)));

        // 5. Dept BBMP, Officer Suresh, Citizen Anand, Status RESOLVED, Priority MEDIUM, HealthScore 100
        complaintRepository.save(createComplaint(5L, "RSK-2026-005", "Pothole on 100ft road", "Deep crater causing traffic jam", "Roads", deptPbbm, officerSuresh, citizenAnand, ComplaintStatus.RESOLVED, Priority.MEDIUM, 100, LocalDateTime.of(2026, 11, 5, 8, 0)));

        // 6. Dept BBMP, Officer Suresh, Citizen Priya, Status REJECTED, Priority LOW, HealthScore 100
        complaintRepository.save(createComplaint(6L, "RSK-2026-006", "Garbage dumped outside layout", "Commercial waste on sidewalk", "Sanitation", deptPbbm, officerSuresh, citizenPriya, ComplaintStatus.REJECTED, Priority.LOW, 100, LocalDateTime.of(2026, 11, 6, 14, 0)));
    }

    private Complaint createComplaint(Long id, String trackingId, String title, String desc, String cat,
                                      Department dept, Officer officer, User citizen,
                                      ComplaintStatus status, Priority priority, int healthScore, LocalDateTime createdAt) {
        Complaint c = Complaint.builder()
                .trackingId(trackingId)
                .trackingNumber(trackingId)
                .title(title)
                .description(desc)
                .category(cat)
                .department(dept)
                .assignedOfficer(officer)
                .citizen(citizen)
                .status(status)
                .priority(priority)
                .healthScore(healthScore)
                .followUpDeadline(createdAt.plusDays(7))
                .slaDueDate(createdAt.plusDays(7))
                .build();
        c.setId(id);
        c.setCreatedAt(createdAt);
        c.setUpdatedAt(createdAt);
        return c;
    }

    // =========================================================================
    // 1. Dashboard Summary Tests
    // =========================================================================

    @Test
    @DisplayName("Dashboard Summary: Admin gets system-wide counts for all statuses")
    void testDashboardSummary_Admin_SystemWide() {
        Authentication auth = new UsernamePasswordAuthenticationToken(adminUserDetails, null, adminUserDetails.getAuthorities());
        ResponseEntity<ApiResponse<DashboardSummaryDto>> resp = dashboardController.getDashboardSummary(auth);

        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isSuccess());
        DashboardSummaryDto summary = resp.getBody().getData();

        assertEquals(6, summary.getTotal());
        assertEquals(1, summary.getNew());
        assertEquals(1, summary.getAssigned());
        assertEquals(1, summary.getInProgress());
        assertEquals(1, summary.getOverdue());
        assertEquals(1, summary.getResolved());
        // Pending = total (6) - resolved (1) - rejected (1) = 4 (NEW, IN_PROGRESS, OVERDUE, ASSIGNED)
        assertEquals(4, summary.getPending());
    }

    @Test
    @DisplayName("Dashboard Summary: Supervisor gets counts scoped to their department (Police)")
    void testDashboardSummary_Supervisor_DepartmentScoped() {
        Authentication auth = new UsernamePasswordAuthenticationToken(supervisorUserDetails, null, supervisorUserDetails.getAuthorities());
        ResponseEntity<ApiResponse<DashboardSummaryDto>> resp = dashboardController.getDashboardSummary(auth);

        DashboardSummaryDto summary = resp.getBody().getData();

        // Dept Police has 4 complaints (001, 002, 003, 004): 1 NEW, 1 IN_PROGRESS, 1 OVERDUE, 1 ASSIGNED
        assertEquals(4, summary.getTotal());
        assertEquals(1, summary.getNew());
        assertEquals(1, summary.getAssigned());
        assertEquals(1, summary.getInProgress());
        assertEquals(1, summary.getOverdue());
        assertEquals(0, summary.getResolved());
        assertEquals(4, summary.getPending());
    }

    @Test
    @DisplayName("Dashboard Summary: Officer gets counts scoped to their assigned complaints (Officer Rajesh)")
    void testDashboardSummary_Officer_AssignedScoped() {
        Authentication auth = new UsernamePasswordAuthenticationToken(officerUserDetails, null, officerUserDetails.getAuthorities());
        ResponseEntity<ApiResponse<DashboardSummaryDto>> resp = dashboardController.getDashboardSummary(auth);

        DashboardSummaryDto summary = resp.getBody().getData();

        // Officer Rajesh has 3 complaints (001, 002, 003): 1 NEW, 1 IN_PROGRESS, 1 OVERDUE
        assertEquals(3, summary.getTotal());
        assertEquals(1, summary.getNew());
        assertEquals(0, summary.getAssigned());
        assertEquals(1, summary.getInProgress());
        assertEquals(1, summary.getOverdue());
        assertEquals(0, summary.getResolved());
        assertEquals(3, summary.getPending());
    }

    @Test
    @DisplayName("Dashboard Summary: Citizen gets counts scoped to their filed complaints (Citizen Anand)")
    void testDashboardSummary_Citizen_CitizenScoped() {
        Authentication auth = new UsernamePasswordAuthenticationToken(citizenUserDetails, null, citizenUserDetails.getAuthorities());
        ResponseEntity<ApiResponse<DashboardSummaryDto>> resp = dashboardController.getDashboardSummary(auth);

        DashboardSummaryDto summary = resp.getBody().getData();

        // Anand filed 3 complaints (001 NEW, 002 IN_PROGRESS, 005 RESOLVED)
        assertEquals(3, summary.getTotal());
        assertEquals(1, summary.getNew());
        assertEquals(0, summary.getAssigned());
        assertEquals(1, summary.getInProgress());
        assertEquals(0, summary.getOverdue());
        assertEquals(1, summary.getResolved());
        assertEquals(2, summary.getPending());
    }

    // =========================================================================
    // 2. Complaint Filters, Free-Text Search, Pagination & HealthScore Sorting
    // =========================================================================

    @Test
    @DisplayName("Search: Sorting by health score ascending puts lowest score (highest breach risk) first")
    void testSearch_SortingByHealthScoreAscending() {
        Authentication auth = new UsernamePasswordAuthenticationToken(adminUserDetails, null, adminUserDetails.getAuthorities());
        ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> resp =
                complaintController.getComplaints(null, null, null, null, null, null, null, null, null, null, 0, 10, "healthScore", "asc", auth);

        List<ComplaintDto.OfficerDetailResponse> list = resp.getBody().getData().getContent();
        assertEquals(6, list.size());

        // First item must have lowest health score (35 - OVERDUE RSK-2026-003)
        assertEquals("RSK-2026-003", list.get(0).getTrackingId());
        assertEquals(35, list.get(0).getHealthScore());

        // Last item must have highest health score (100)
        assertEquals(100, list.get(list.size() - 1).getHealthScore());
    }

    @Test
    @DisplayName("Search: Sorting by health score descending puts highest score first")
    void testSearch_SortingByHealthScoreDescending() {
        Authentication auth = new UsernamePasswordAuthenticationToken(adminUserDetails, null, adminUserDetails.getAuthorities());
        ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> resp =
                complaintController.getComplaints(null, null, null, null, null, null, null, null, null, null, 0, 10, "healthScore", "desc", auth);

        List<ComplaintDto.OfficerDetailResponse> list = resp.getBody().getData().getContent();
        assertEquals(100, list.get(0).getHealthScore());
        assertEquals(35, list.get(list.size() - 1).getHealthScore());
    }

    @Test
    @DisplayName("Search: Filter by department ID restricts results to department")
    void testSearch_FilterByDepartment() {
        Authentication auth = new UsernamePasswordAuthenticationToken(adminUserDetails, null, adminUserDetails.getAuthorities());
        ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> resp =
                complaintController.getComplaints(null, 2L, null, null, null, null, null, null, null, null, 0, 10, "healthScore", "asc", auth);

        List<ComplaintDto.OfficerDetailResponse> list = resp.getBody().getData().getContent();
        assertEquals(2, list.size());
        assertTrue(list.stream().allMatch(c -> c.getDepartmentId().equals(2L)));
    }

    @Test
    @DisplayName("Search: Filter by status returns only complaints with that status")
    void testSearch_FilterByStatus() {
        Authentication auth = new UsernamePasswordAuthenticationToken(adminUserDetails, null, adminUserDetails.getAuthorities());
        ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> resp =
                complaintController.getComplaints(null, null, "OVERDUE", null, null, null, null, null, null, null, 0, 10, "healthScore", "asc", auth);

        List<ComplaintDto.OfficerDetailResponse> list = resp.getBody().getData().getContent();
        assertEquals(1, list.size());
        assertEquals("RSK-2026-003", list.get(0).getTrackingId());
        assertEquals(ComplaintStatus.OVERDUE, list.get(0).getStatus());
    }

    @Test
    @DisplayName("Search: Filter by priority returns only complaints with matching priority")
    void testSearch_FilterByPriority() {
        Authentication auth = new UsernamePasswordAuthenticationToken(adminUserDetails, null, adminUserDetails.getAuthorities());
        ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> resp =
                complaintController.getComplaints(null, null, null, "HIGH", null, null, null, null, null, null, 0, 10, "healthScore", "asc", auth);

        List<ComplaintDto.OfficerDetailResponse> list = resp.getBody().getData().getContent();
        assertEquals(1, list.size());
        assertEquals("RSK-2026-001", list.get(0).getTrackingId());
        assertEquals(Priority.HIGH, list.get(0).getPriority());
    }

    @Test
    @DisplayName("Search: Filter by date range returns complaints within range")
    void testSearch_FilterByDateRange() {
        Authentication auth = new UsernamePasswordAuthenticationToken(adminUserDetails, null, adminUserDetails.getAuthorities());
        // Range Nov 2 to Nov 4
        ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> resp =
                complaintController.getComplaints(null, null, null, null, "2026-11-02", "2026-11-04", null, null, null, null, 0, 10, "healthScore", "asc", auth);

        List<ComplaintDto.OfficerDetailResponse> list = resp.getBody().getData().getContent();
        assertEquals(3, list.size()); // 002 (Nov 2), 003 (Nov 3), 004 (Nov 4)
        assertTrue(list.stream().allMatch(c -> c.getCreatedAt().isAfter(LocalDateTime.of(2026, 11, 1, 23, 59))
                && c.getCreatedAt().isBefore(LocalDateTime.of(2026, 11, 5, 0, 0))));
    }

    @Test
    @DisplayName("Search: Free-text search matches title, description, citizen name, and tracking ID")
    void testSearch_FreeText() {
        Authentication auth = new UsernamePasswordAuthenticationToken(adminUserDetails, null, adminUserDetails.getAuthorities());

        // 1. Search by title keyword "pothole"
        ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> resp1 =
                complaintController.getComplaints(null, null, null, null, null, null, null, null, "pothole", null, 0, 10, "healthScore", "asc", auth);
        assertEquals(1, resp1.getBody().getData().getContent().size());
        assertEquals("RSK-2026-005", resp1.getBody().getData().getContent().get(0).getTrackingId());

        // 2. Search by description keyword "lottery scam"
        ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> resp2 =
                complaintController.getComplaints(null, null, null, null, null, null, null, null, null, "lottery scam", 0, 10, "healthScore", "asc", auth);
        assertEquals(1, resp2.getBody().getData().getContent().size());
        assertEquals("RSK-2026-002", resp2.getBody().getData().getContent().get(0).getTrackingId());

        // 3. Search by citizen name "Priya"
        ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> resp3 =
                complaintController.getComplaints(null, null, null, null, null, null, null, null, "Priya", null, 0, 10, "healthScore", "asc", auth);
        assertEquals(3, resp3.getBody().getData().getContent().size()); // 003, 004, 006

        // 4. Search by tracking ID "001"
        ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> resp4 =
                complaintController.getComplaints(null, null, null, null, null, null, null, null, "RSK-2026-001", null, 0, 10, "healthScore", "asc", auth);
        assertEquals(1, resp4.getBody().getData().getContent().size());
        assertEquals("RSK-2026-001", resp4.getBody().getData().getContent().get(0).getTrackingId());
    }

    @Test
    @DisplayName("Search: Pagination splits results across pages correctly")
    void testSearch_Pagination() {
        Authentication auth = new UsernamePasswordAuthenticationToken(adminUserDetails, null, adminUserDetails.getAuthorities());

        // Page 0, size 2
        ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> respPage0 =
                complaintController.getComplaints(null, null, null, null, null, null, null, null, null, null, 0, 2, "healthScore", "asc", auth);
        PageResponse<ComplaintDto.OfficerDetailResponse> page0 = respPage0.getBody().getData();
        assertEquals(2, page0.getContent().size());
        assertEquals(6, page0.getTotalElements());
        assertEquals(3, page0.getTotalPages());
        assertTrue(page0.isFirst());
        assertFalse(page0.isLast());

        // Page 1, size 2
        ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> respPage1 =
                complaintController.getComplaints(null, null, null, null, null, null, null, null, null, null, 1, 2, "healthScore", "asc", auth);
        PageResponse<ComplaintDto.OfficerDetailResponse> page1 = respPage1.getBody().getData();
        assertEquals(2, page1.getContent().size());
        assertFalse(page1.isFirst());
        assertFalse(page1.isLast());

        // Page 2, size 2 (last page)
        ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> respPage2 =
                complaintController.getComplaints(null, null, null, null, null, null, null, null, null, null, 2, 2, "healthScore", "asc", auth);
        PageResponse<ComplaintDto.OfficerDetailResponse> page2 = respPage2.getBody().getData();
        assertEquals(2, page2.getContent().size());
        assertTrue(page2.isLast());
    }

    @Test
    @DisplayName("Search: Supervisor caller is automatically scoped to their department")
    void testSearch_Supervisor_DepartmentScoped() {
        Authentication auth = new UsernamePasswordAuthenticationToken(supervisorUserDetails, null, supervisorUserDetails.getAuthorities());
        ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> resp =
                complaintController.getComplaints(null, null, null, null, null, null, null, null, null, null, 0, 10, "healthScore", "asc", auth);

        List<ComplaintDto.OfficerDetailResponse> list = resp.getBody().getData().getContent();
        assertEquals(4, list.size());
        assertTrue(list.stream().allMatch(c -> c.getDepartmentId().equals(deptPolice.getId())));
    }

    // =========================================================================
    // In-Memory Test Double
    // =========================================================================

    private static class InMemoryComplaintRepository implements ComplaintRepository {
        private final Map<Long, Complaint> store = new LinkedHashMap<>();

        @Override
        public synchronized <S extends Complaint> S save(S entity) {
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override public Optional<Complaint> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<Complaint> findByTrackingId(String trackingId) { return store.values().stream().filter(c -> trackingId.equals(c.getTrackingId())).findFirst(); }
        @Override public Optional<Complaint> findByTrackingNumber(String trackingNumber) { return store.values().stream().filter(c -> trackingNumber.equals(c.getTrackingNumber())).findFirst(); }
        @Override public Optional<Complaint> findByTrackingIdOrTrackingNumber(String id, String num) { return findByTrackingId(id); }
        @Override public List<Complaint> findAllByCitizenIdOrderByCreatedAtDesc(Long citizenId) { return store.values().stream().filter(c -> c.getCitizen() != null && citizenId.equals(c.getCitizen().getId())).toList(); }
        @Override public List<Complaint> findAllByDepartmentIdOrderByCreatedAtDesc(Long departmentId) { return store.values().stream().filter(c -> c.getDepartment() != null && departmentId.equals(c.getDepartment().getId())).toList(); }
        @Override public List<Complaint> findAllByAssignedOfficerIdOrderByCreatedAtDesc(Long assignedOfficerId) { return store.values().stream().filter(c -> c.getAssignedOfficer() != null && assignedOfficerId.equals(c.getAssignedOfficer().getId())).toList(); }
        @Override public List<Complaint> findAllByStatus(ComplaintStatus status) { return store.values().stream().filter(c -> status == c.getStatus()).toList(); }
        @Override public List<Complaint> findAllByDepartmentIdAndStatus(Long departmentId, ComplaintStatus status) { return Collections.emptyList(); }
        @Override public List<Complaint> findComplaintsNearingDeadline(LocalDateTime threshold, LocalDateTime now) { return Collections.emptyList(); }
        @Override public List<Complaint> findOverdueForEscalation(LocalDateTime now, int maxLevel) { return Collections.emptyList(); }
        @Override public long countByStatus(ComplaintStatus status) { return store.values().stream().filter(c -> status == c.getStatus()).count(); }
        @Override public long countByDepartmentId(Long departmentId) { return store.values().stream().filter(c -> c.getDepartment() != null && departmentId.equals(c.getDepartment().getId())).count(); }
        @Override public long countByAssignedOfficerId(Long assignedOfficerId) { return store.values().stream().filter(c -> c.getAssignedOfficer() != null && assignedOfficerId.equals(c.getAssignedOfficer().getId())).count(); }

        @Override
        public Page<Complaint> searchComplaints(Long deptId, Long officerId, Long citizenId,
                                                ComplaintStatus status, Priority priority,
                                                LocalDateTime startDate, LocalDateTime endDate,
                                                String search, Pageable pageable) {
            List<Complaint> filtered = store.values().stream()
                    .filter(c -> deptId == null || (c.getDepartment() != null && deptId.equals(c.getDepartment().getId())))
                    .filter(c -> officerId == null || (c.getAssignedOfficer() != null && officerId.equals(c.getAssignedOfficer().getId())))
                    .filter(c -> citizenId == null || (c.getCitizen() != null && citizenId.equals(c.getCitizen().getId())))
                    .filter(c -> status == null || status.equals(c.getStatus()))
                    .filter(c -> priority == null || priority.equals(c.getPriority()))
                    .filter(c -> startDate == null || (c.getCreatedAt() != null && !c.getCreatedAt().isBefore(startDate)))
                    .filter(c -> endDate == null || (c.getCreatedAt() != null && !c.getCreatedAt().isAfter(endDate)))
                    .filter(c -> {
                        if (search == null || search.isBlank()) return true;
                        String q = search.replace("%", "").toLowerCase();
                        boolean matchesTitle = c.getTitle() != null && c.getTitle().toLowerCase().contains(q);
                        boolean matchesDesc = c.getDescription() != null && c.getDescription().toLowerCase().contains(q);
                        boolean matchesTracking = (c.getTrackingId() != null && c.getTrackingId().toLowerCase().contains(q))
                                || (c.getTrackingNumber() != null && c.getTrackingNumber().toLowerCase().contains(q));
                        boolean matchesCat = c.getCategory() != null && c.getCategory().toLowerCase().contains(q);
                        boolean matchesCitizen = c.getCitizen() != null && c.getCitizen().getFullName() != null && c.getCitizen().getFullName().toLowerCase().contains(q);
                        return matchesTitle || matchesDesc || matchesTracking || matchesCat || matchesCitizen;
                    })
                    .sorted((c1, c2) -> {
                        Sort.Order order = pageable.getSort().getOrderFor("healthScore");
                        boolean asc = order == null || order.isAscending();
                        int h1 = c1.getHealthScore() != null ? c1.getHealthScore() : 100;
                        int h2 = c2.getHealthScore() != null ? c2.getHealthScore() : 100;
                        return asc ? Integer.compare(h1, h2) : Integer.compare(h2, h1);
                    })
                    .toList();

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filtered.size());
            List<Complaint> pageContent = (start <= end && start < filtered.size()) ? filtered.subList(start, end) : Collections.emptyList();
            return new PageImpl<>(pageContent, pageable, filtered.size());
        }

        @Override
        public List<Object[]> countByStatusAll() {
            return groupStatus(store.values());
        }

        @Override
        public List<Object[]> countByStatusForDepartment(Long deptId) {
            return groupStatus(store.values().stream().filter(c -> c.getDepartment() != null && deptId.equals(c.getDepartment().getId())).toList());
        }

        @Override
        public List<Object[]> countByStatusForOfficer(Long officerId) {
            return groupStatus(store.values().stream().filter(c -> c.getAssignedOfficer() != null && officerId.equals(c.getAssignedOfficer().getId())).toList());
        }

        @Override
        public List<Object[]> countByStatusForCitizen(Long citizenId) {
            return groupStatus(store.values().stream().filter(c -> c.getCitizen() != null && citizenId.equals(c.getCitizen().getId())).toList());
        }

        @Override public List<Complaint> findOpenByDepartmentAndCategorySince(Long deptId, String category, LocalDateTime since) { return Collections.emptyList(); }
        @Override public List<Complaint> findAllByCommunityIssueIdOrderByCreatedAtAsc(Long communityIssueId) { return Collections.emptyList(); }
        @Override public List<Complaint> findAwaitingResolutionReviewBefore(LocalDateTime cutoff) { return Collections.emptyList(); }

        private List<Object[]> groupStatus(Collection<Complaint> collection) {
            Map<ComplaintStatus, Long> counts = collection.stream()
                    .filter(c -> c.getStatus() != null)
                    .collect(Collectors.groupingBy(Complaint::getStatus, Collectors.counting()));
            List<Object[]> rows = new ArrayList<>();
            for (Map.Entry<ComplaintStatus, Long> e : counts.entrySet()) {
                rows.add(new Object[]{e.getKey(), e.getValue()});
            }
            return rows;
        }

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
        @Override public <S extends Complaint, R> R findBy(Example<S> example, Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
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
}

