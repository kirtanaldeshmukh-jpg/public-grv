package com.resolvetrack.common;

import com.resolvetrack.complaint.ComplaintDto;
import com.resolvetrack.complaint.ComplaintService;
import com.resolvetrack.complaint.ComplaintStatus;
import com.resolvetrack.department.DepartmentDto;
import com.resolvetrack.department.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PublicControllerTest {

    private PublicController publicController;
    private StubComplaintService complaintService;
    private StubDepartmentService departmentService;

    @BeforeEach
    void setUp() {
        complaintService = new StubComplaintService();
        departmentService = new StubDepartmentService();
        publicController = new PublicController(complaintService, departmentService);
    }

    @Test
    @DisplayName("Public complaint lodgement should succeed and return tracking id")
    void testSubmitPublicComplaint_Success() {
        ComplaintDto.CreateRequest request = ComplaintDto.CreateRequest.builder()
                .departmentId(1L)
                .title("Noise nuisance in residential area")
                .description("Loud loudspeakers beyond 10 PM")
                .category("Law and Order")
                .citizenName("Citizen Sneha")
                .citizenEmail("sneha@example.com")
                .build();

        ResponseEntity<ApiResponse<ComplaintDto.CitizenTrackingResponse>> response =
                publicController.submitPublicComplaintJson(request);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("RSK-2026-001", response.getBody().getData().getTrackingId());
    }

    @Test
    @DisplayName("GET /api/public/track/{id} returns only status, department, latest update, and resolved or pending")
    void testTrackPublicComplaint_MinimalPrivacyView() {
        ResponseEntity<ApiResponse<ComplaintDto.PublicTrackResponse>> response =
                publicController.trackPublicComplaint("RSK-2026-001");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());

        ComplaintDto.PublicTrackResponse data = response.getBody().getData();
        assertEquals("NEW", data.getStatus());
        assertEquals("Police Department", data.getDepartmentName());
        assertEquals("Grievance received and registered under Karnataka SAKALA Act", data.getLatestPublicUpdate());
        assertEquals("PENDING", data.getResolvedOrPending());
        assertFalse(data.isResolved());
    }

    @Test
    @DisplayName("Public departments endpoint should list active departments")
    void testGetPublicDepartments_Success() {
        ResponseEntity<ApiResponse<List<DepartmentDto.Response>>> response =
                publicController.getPublicDepartments();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("POL", response.getBody().getData().get(0).getCode());
    }

    @Test
    @DisplayName("Public health check returns UP")
    void testHealthCheck_ReturnsUp() {
        ResponseEntity<ApiResponse<Map<String, String>>> response = publicController.healthCheck();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().getData().get("status"));
    }

    // =========================================================================
    // Stubs for PublicController test
    // =========================================================================

    private static class StubComplaintService extends ComplaintService {
        public StubComplaintService() {
            super(null, null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public ComplaintDto.CitizenTrackingResponse registerPublicComplaint(ComplaintDto.CreateRequest request, java.util.List<org.springframework.web.multipart.MultipartFile> files) {
            return ComplaintDto.CitizenTrackingResponse.builder()
                    .trackingId("RSK-2026-001")
                    .trackingNumber("RSK-2026-001")
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .category(request.getCategory())
                    .departmentNameEn("Police Department")
                    .departmentNameKn("ಪೊಲೀಸ್ ಇಲಾಖೆ")
                    .status(ComplaintStatus.NEW)
                    .followUpDeadline(LocalDateTime.now().plusDays(7))
                    .slaDueDate(LocalDateTime.now().plusDays(7))
                    .createdAt(LocalDateTime.now())
                    .escalationLevel(0)
                    .timeline(Collections.emptyList())
                    .build();
        }

        @Override
        public ComplaintDto.PublicTrackResponse getPublicTrackInfo(String idOrRef) {
            return ComplaintDto.PublicTrackResponse.builder()
                    .status("NEW")
                    .departmentName("Police Department")
                    .latestPublicUpdate("Grievance received and registered under Karnataka SAKALA Act")
                    .resolvedOrPending("PENDING")
                    .resolved(false)
                    .build();
        }

        @Override
        public ComplaintDto.CitizenTrackingResponse getCitizenTrackingByNumber(String trackingRef) {
            return ComplaintDto.CitizenTrackingResponse.builder()
                    .trackingId(trackingRef)
                    .trackingNumber(trackingRef)
                    .title("Noise nuisance in residential area")
                    .description("Loud loudspeakers beyond 10 PM")
                    .category("Law and Order")
                    .departmentNameEn("Police Department")
                    .departmentNameKn("ಪೊಲೀಸ್ ಇಲಾಖೆ")
                    .status(ComplaintStatus.NEW)
                    .followUpDeadline(LocalDateTime.now().plusDays(7))
                    .slaDueDate(LocalDateTime.now().plusDays(7))
                    .createdAt(LocalDateTime.now())
                    .escalationLevel(0)
                    .timeline(Collections.emptyList())
                    .build();
        }
    }

    private static class StubDepartmentService extends DepartmentService {
        public StubDepartmentService() {
            super(null);
        }

        @Override
        public List<DepartmentDto.Response> getActiveDepartments() {
            return List.of(DepartmentDto.Response.builder()
                    .id(1L)
                    .code("POL")
                    .nameEn("Police Department")
                    .nameKn("ಪೊಲೀಸ್ ಇಲಾಖೆ")
                    .defaultSlaDays(7)
                    .escalationHours(24)
                    .active(true)
                    .build());
        }
    }
}
