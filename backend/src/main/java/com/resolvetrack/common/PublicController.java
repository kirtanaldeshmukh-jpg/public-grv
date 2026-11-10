package com.resolvetrack.common;

import com.resolvetrack.complaint.ComplaintDto;
import com.resolvetrack.complaint.ComplaintService;
import com.resolvetrack.department.DepartmentDto;
import com.resolvetrack.department.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = {"/api/public", "/api/v1/public"})
public class PublicController {

    private final ComplaintService complaintService;
    private final DepartmentService departmentService;

    public PublicController(ComplaintService complaintService, DepartmentService departmentService) {
        this.complaintService = complaintService;
        this.departmentService = departmentService;
    }

    /**
     * Public complaint registration with optional multipart images and PDFs up to 5 MB.
     * Generates tracking ID RSK-{year}-{seq}, sets status NEW, computes deadline from settings by category,
     * writes first action_history row.
     */
    @PostMapping(value = "/complaints", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<ComplaintDto.CitizenTrackingResponse>> submitPublicComplaintMultipart(
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam(value = "priority", required = false) com.resolvetrack.complaint.Priority priority,
            @RequestParam(value = "citizenName", required = false) String citizenName,
            @RequestParam(value = "citizenPhone", required = false) String citizenPhone,
            @RequestParam(value = "citizenEmail", required = false) String citizenEmail,
            @RequestParam(value = "locationAddress", required = false) String locationAddress,
            @RequestParam(value = "latitude", required = false) BigDecimal latitude,
            @RequestParam(value = "longitude", required = false) BigDecimal longitude,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        ComplaintDto.CreateRequest request = ComplaintDto.CreateRequest.builder()
                .departmentId(departmentId)
                .title(title)
                .description(description)
                .category(category)
                .priority(priority)
                .citizenName(citizenName)
                .citizenPhone(citizenPhone)
                .citizenEmail(citizenEmail)
                .locationAddress(locationAddress)
                .latitude(latitude)
                .longitude(longitude)
                .build();

        ComplaintDto.CitizenTrackingResponse response = complaintService.registerPublicComplaint(request, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Grievance lodged successfully", response));
    }

    /**
     * JSON payload fallback for public complaint registration.
     */
    @PostMapping(value = "/complaints", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ApiResponse<ComplaintDto.CitizenTrackingResponse>> submitPublicComplaintJson(
            @Valid @RequestBody ComplaintDto.CreateRequest request) {
        ComplaintDto.CitizenTrackingResponse response = complaintService.registerPublicComplaint(request, Collections.emptyList());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Grievance lodged successfully", response));
    }

    /**
     * Public tracking endpoint returning ONLY:
     * - status
     * - department name
     * - latest public update
     * - resolved or pending
     * Citizens never see officer names, internal notes, or other citizens' data.
     */
    @GetMapping("/track/{id}")
    public ResponseEntity<ApiResponse<ComplaintDto.PublicTrackResponse>> trackPublicComplaint(
            @PathVariable("id") String id) {
        ComplaintDto.PublicTrackResponse response = complaintService.getPublicTrackInfo(id);
        return ResponseEntity.ok(ApiResponse.ok("Public tracking information retrieved", response));
    }

    /**
     * Backward-compatible tracking endpoint.
     */
    @GetMapping("/complaints/track/{trackingRef}")
    public ResponseEntity<ApiResponse<ComplaintDto.CitizenTrackingResponse>> trackPublicComplaintDetail(
            @PathVariable("trackingRef") String trackingRef) {
        ComplaintDto.CitizenTrackingResponse tracking = complaintService.getCitizenTrackingByNumber(trackingRef);
        return ResponseEntity.ok(ApiResponse.ok(tracking));
    }

    /**
     * Citizen confirms resolution -> transitions to RESOLVED (Prompt 7.3).
     */
    @PostMapping(path = {"/complaints/{trackingId}/confirm-resolution", "/track/{trackingId}/confirm-resolution"})
    public ResponseEntity<ApiResponse<ComplaintDto.CitizenTrackingResponse>> confirmResolution(
            @PathVariable("trackingId") String trackingId,
            @RequestBody(required = false) ComplaintDto.ConfirmResolutionRequest request) {
        ComplaintDto.CitizenTrackingResponse response = complaintService.confirmResolutionByCitizen(
                trackingId, request != null ? request : new ComplaintDto.ConfirmResolutionRequest());
        return ResponseEntity.ok(ApiResponse.ok("Resolution confirmed. Grievance closed successfully.", response));
    }

    /**
     * Citizen rejects resolution -> transitions to REOPENED (Prompt 7.3).
     */
    @PostMapping(path = {"/complaints/{trackingId}/reject-resolution", "/track/{trackingId}/reject-resolution"})
    public ResponseEntity<ApiResponse<ComplaintDto.CitizenTrackingResponse>> rejectResolution(
            @PathVariable("trackingId") String trackingId,
            @Valid @RequestBody ComplaintDto.RejectResolutionRequest request) {
        ComplaintDto.CitizenTrackingResponse response = complaintService.rejectResolutionByCitizen(trackingId, request);
        return ResponseEntity.ok(ApiResponse.ok("Resolution rejected. Grievance reopened for departmental review.", response));
    }

    /**
     * Public active departments for grievance dropdowns.
     */
    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<DepartmentDto.Response>>> getPublicDepartments() {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.getActiveDepartments()));
    }

    /**
     * Public system health check.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "status", "UP",
                "framework", "Karnataka SAKALA Services Act (2011)",
                "system", "ResolveTrack Grievance Portal"
        )));
    }
}
