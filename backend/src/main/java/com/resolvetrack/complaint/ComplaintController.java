package com.resolvetrack.complaint;

import com.resolvetrack.auth.CustomUserDetails;
import com.resolvetrack.auth.User;
import com.resolvetrack.auth.UserRepository;
import com.resolvetrack.common.ApiResponse;
import com.resolvetrack.common.PageResponse;
import com.resolvetrack.department.DepartmentSuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = {"/api/complaints", "/api/v1/complaints"})
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserRepository userRepository;
    private final DepartmentSuggestionService departmentSuggestionService;

    public ComplaintController(
            ComplaintService complaintService,
            UserRepository userRepository,
            DepartmentSuggestionService departmentSuggestionService) {
        this.complaintService = complaintService;
        this.userRepository = userRepository;
        this.departmentSuggestionService = departmentSuggestionService;
    }

    /**
     * Official grievance registration for officials (source: WEB, PHONE, PAPER).
     * Generates tracking ID RSK-{year}-{seq}, status NEW, computes deadline from settings,
     * writes first action_history row.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'OFFICER', 'DATA_ENTRY')")
    public ResponseEntity<ApiResponse<ComplaintDto.OfficerDetailResponse>> submitOfficialComplaint(
            @Valid @RequestBody ComplaintDto.CreateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        ComplaintDto.OfficerDetailResponse response = complaintService.registerOfficialComplaint(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Grievance registered successfully by official", response));
    }

    /**
     * Assign or reassign complaint to an officer with justification.
     * Allowed for ADMIN and SUPERVISOR.
     * Updates status to ASSIGNED, notifies the officer, and writes action_history.
     */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPERVISOR') and @complaintSecurity.canAccessComplaint(authentication, #id))")
    public ResponseEntity<ApiResponse<Void>> assignComplaint(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintDto.AssignOfficerRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        Long actorId = user != null ? user.getId() : null;
        String actorRole = user != null ? user.getRole().name() : "SUPERVISOR";

        complaintService.assignOfficer(id, request, actorId, username, actorRole);
        return ResponseEntity.ok(ApiResponse.ok("Complaint assigned successfully to officer", null));
    }

    /**
     * Backward-compatible POST assign endpoint.
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPERVISOR') and @complaintSecurity.canAccessComplaint(authentication, #id))")
    public ResponseEntity<ApiResponse<Void>> assignOfficerPost(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintDto.AssignOfficerRequest request,
            Authentication authentication) {
        return assignComplaint(id, request, authentication);
    }

    /**
     * Department suggestion endpoint using keyword mapper (English & Kannada).
     * The suggestion is only assistive; assignment requires an authorized user.
     */
    @GetMapping("/suggest-department")
    public ResponseEntity<ApiResponse<DepartmentSuggestionService.SuggestionResult>> suggestDepartment(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "text", required = false) String text) {
        DepartmentSuggestionService.SuggestionResult suggestion = departmentSuggestionService.suggestDepartment(category, text);
        return ResponseEntity.ok(ApiResponse.ok("Department suggestion generated", suggestion));
    }

    /**
     * AI-assisted triage suggestion endpoint (Prompt 8.2).
     * Returns category, department, priority, issue summary, extracted location, and confidence.
     * Always returned as assistive suggestion that an official can change.
     */
    @PostMapping("/suggest-triage")
    public ResponseEntity<ApiResponse<DepartmentSuggestionService.SuggestionResult>> suggestTriage(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "category", required = false) String categoryParam,
            @RequestParam(value = "text", required = false) String textParam) {
        String category = body != null && body.containsKey("category") ? body.get("category") : categoryParam;
        String title = body != null && body.containsKey("title") ? body.get("title") : "";
        String description = body != null && body.containsKey("description") ? body.get("description") : (textParam != null ? textParam : "");
        String location = body != null && body.containsKey("locationAddress") ? body.get("locationAddress") : "";

        DepartmentSuggestionService.SuggestionResult suggestion = departmentSuggestionService.suggest(category, title, description, location);
        return ResponseEntity.ok(ApiResponse.ok("Triage suggestion generated", suggestion));
    }

    /**
     * Get complaints filtered by department, status, priority, date range, and free-text search,
     * with pagination and sorting by health score (scoped by caller's role).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'OFFICER', 'DATA_ENTRY', 'CITIZEN')")
    public ResponseEntity<ApiResponse<PageResponse<ComplaintDto.OfficerDetailResponse>>> getComplaints(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "healthScore") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // 1. Resolve department filter
        Long resolvedDeptId = departmentId;
        if (resolvedDeptId == null && department != null && !department.isBlank()) {
            try {
                resolvedDeptId = Long.parseLong(department.trim());
            } catch (NumberFormatException ignored) {}
        }

        // 2. Resolve status
        ComplaintStatus resolvedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                resolvedStatus = ComplaintStatus.valueOf(status.trim().toUpperCase().replace("-", "_"));
            } catch (IllegalArgumentException ignored) {}
        }

        // 3. Resolve priority
        Priority resolvedPriority = null;
        if (priority != null && !priority.isBlank()) {
            try {
                resolvedPriority = Priority.valueOf(priority.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        // 4. Resolve date range
        String startStr = (startDate != null && !startDate.isBlank()) ? startDate : from;
        String endStr = (endDate != null && !endDate.isBlank()) ? endDate : to;
        LocalDateTime parsedStart = parseDateTime(startStr, false);
        LocalDateTime parsedEnd = parseDateTime(endStr, true);

        // 5. Resolve search text
        String searchText = (search != null && !search.isBlank()) ? search : q;

        ComplaintDto.FilterRequest filterRequest = ComplaintDto.FilterRequest.builder()
                .departmentId(resolvedDeptId)
                .status(resolvedStatus)
                .priority(resolvedPriority)
                .startDate(parsedStart)
                .endDate(parsedEnd)
                .search(searchText)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();

        PageResponse<ComplaintDto.OfficerDetailResponse> response = complaintService.searchComplaints(filterRequest, userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Complaints retrieved successfully", response));
    }

    private LocalDateTime parseDateTime(String input, boolean isEndOfDay) {
        if (input == null || input.isBlank()) return null;
        String trimmed = input.trim();
        try {
            if (trimmed.length() == 10) { // YYYY-MM-DD
                LocalDate ld = LocalDate.parse(trimmed);
                return isEndOfDay ? ld.atTime(LocalTime.MAX) : ld.atStartOfDay();
            }
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(trimmed);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @GetMapping("/track/{trackingRef}")
    public ResponseEntity<ApiResponse<ComplaintDto.CitizenTrackingResponse>> trackComplaint(
            @PathVariable String trackingRef) {
        return ResponseEntity.ok(ApiResponse.ok(complaintService.getCitizenTrackingByNumber(trackingRef)));
    }

    /**
     * Method-level security:
     * - Admins see any complaint
     * - Supervisors see complaints in their department
     * - Officers see complaints assigned to them
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @complaintSecurity.canAccessComplaint(authentication, #id)")
    public ResponseEntity<ApiResponse<ComplaintDto.OfficerDetailResponse>> getComplaintDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(complaintService.getOfficerDetail(id)));
    }

    /**
     * Officer progress update for complaint.
     * Resets deadline, acknowledges open reminders, clears escalation flags when appropriate,
     * recalculates health score, and writes action_history.
     * Only assigned officer, their supervisor or admin may update.
     */
    @PostMapping("/{id}/updates")
    @PreAuthorize("@complaintSecurity.canUpdateComplaint(authentication, #id)")
    public ResponseEntity<ApiResponse<ComplaintDto.OfficerDetailResponse>> updateComplaintProgress(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintDto.ProgressUpdateRequest request,
            Authentication authentication) {
        ComplaintDto.OfficerDetailResponse response = complaintService.updateProgress(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Complaint progress updated successfully", response));
    }

    /**
     * Audit history for complaint.
     * Citizens only see non-internal history with masked officer details;
     * Staff see complete internal history.
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<?>>> getComplaintHistory(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isStaff = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPERVISOR")
                        || a.getAuthority().equals("ROLE_OFFICER")
                        || a.getAuthority().equals("ROLE_DATA_ENTRY"));

        if (isStaff) {
            return ResponseEntity.ok(ApiResponse.ok(complaintService.getInternalHistory(id)));
        } else {
            return ResponseEntity.ok(ApiResponse.ok(complaintService.getCitizenHistory(id)));
        }
    }

    @RequestMapping(path = "/{id}/status", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasRole('ADMIN') or (hasRole('OFFICER') and @complaintSecurity.canUpdateComplaint(authentication, #id)) or (hasRole('SUPERVISOR') and @complaintSecurity.canAccessComplaint(authentication, #id))")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintDto.StatusUpdateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        Long actorId = user != null ? user.getId() : null;
        String role = user != null ? user.getRole().name() : "OFFICER";

        complaintService.updateStatus(id, request, actorId, username, role);
        return ResponseEntity.ok(ApiResponse.ok("Complaint status updated successfully", null));
    }

    @PostMapping("/{id}/confirm-ai-triage")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPERVISOR') and @complaintSecurity.canAccessComplaint(authentication, #id)) or (hasRole('OFFICER') and @complaintSecurity.canAccessComplaint(authentication, #id))")
    public ResponseEntity<ApiResponse<Void>> confirmAiTriage(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintDto.ConfirmAiTriageRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        Long actorId = user != null ? user.getId() : null;

        complaintService.confirmAiTriage(id, request, actorId, username);
        return ResponseEntity.ok(ApiResponse.ok("AI recommendation confirmed by official", null));
    }

    @GetMapping("/officer/{officerId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPERVISOR') and @complaintSecurity.canAccessDepartment(authentication, #officerId)) or (hasRole('OFFICER') and authentication.principal.officerId == #officerId)")
    public ResponseEntity<ApiResponse<List<ComplaintDto.OfficerDetailResponse>>> getOfficerWorkload(
            @PathVariable Long officerId) {
        return ResponseEntity.ok(ApiResponse.ok(complaintService.getOfficerWorkload(officerId)));
    }

    /**
     * Submit verified resolution with summary and proof attachments (multipart) - Prompt 7.3.
     */
    @PostMapping(value = "/{id}/resolve", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasRole('ADMIN') or (hasRole('OFFICER') and @complaintSecurity.canUpdateComplaint(authentication, #id)) or (hasRole('SUPERVISOR') and @complaintSecurity.canAccessComplaint(authentication, #id))")
    public ResponseEntity<ApiResponse<ComplaintDto.OfficerDetailResponse>> submitResolutionMultipart(
            @PathVariable Long id,
            @RequestParam("summary") String summary,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) {
        String username = authentication.getName();
        ComplaintDto.SubmitResolutionRequest req = new ComplaintDto.SubmitResolutionRequest(summary);
        ComplaintDto.OfficerDetailResponse response = complaintService.submitResolution(id, req, files, username);
        return ResponseEntity.ok(ApiResponse.ok("Resolution proof submitted successfully for citizen sign-off", response));
    }

    /**
     * Submit verified resolution with summary (JSON payload) - Prompt 7.3.
     */
    @PostMapping(value = "/{id}/resolve", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('ADMIN') or (hasRole('OFFICER') and @complaintSecurity.canUpdateComplaint(authentication, #id)) or (hasRole('SUPERVISOR') and @complaintSecurity.canAccessComplaint(authentication, #id))")
    public ResponseEntity<ApiResponse<ComplaintDto.OfficerDetailResponse>> submitResolutionJson(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintDto.SubmitResolutionRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        ComplaintDto.OfficerDetailResponse response = complaintService.submitResolution(id, request, Collections.emptyList(), username);
        return ResponseEntity.ok(ApiResponse.ok("Resolution proof submitted successfully for citizen sign-off", response));
    }
}
