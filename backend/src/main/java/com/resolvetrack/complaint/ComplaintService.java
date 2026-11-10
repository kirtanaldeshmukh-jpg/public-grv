package com.resolvetrack.complaint;

import com.resolvetrack.auth.CustomUserDetails;
import com.resolvetrack.auth.Role;
import com.resolvetrack.auth.User;
import com.resolvetrack.auth.UserRepository;
import com.resolvetrack.common.BusinessRuleException;
import com.resolvetrack.common.PageResponse;
import com.resolvetrack.common.ResourceNotFoundException;
import com.resolvetrack.common.SystemSettingRepository;
import com.resolvetrack.department.Department;
import com.resolvetrack.department.DepartmentRepository;
import com.resolvetrack.department.DepartmentSuggestionService;
import com.resolvetrack.escalation.Escalation;
import com.resolvetrack.escalation.EscalationRepository;
import com.resolvetrack.history.ActionHistory;
import com.resolvetrack.history.ActionHistoryDto;
import com.resolvetrack.history.ActionHistoryRepository;
import com.resolvetrack.history.ActionHistoryService;
import com.resolvetrack.notification.NotificationService;
import com.resolvetrack.officer.Officer;
import com.resolvetrack.officer.OfficerRepository;
import com.resolvetrack.reminder.HealthScoreResult;
import com.resolvetrack.reminder.HealthScoreService;
import com.resolvetrack.reminder.Reminder;
import com.resolvetrack.reminder.ReminderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final OfficerRepository officerRepository;
    private final ActionHistoryService actionHistoryService;
    private final ActionHistoryRepository actionHistoryRepository;
    private final TrackingIdGeneratorService trackingIdGeneratorService;
    private final FileUploadService fileUploadService;
    private final DepartmentSuggestionService departmentSuggestionService;
    private final NotificationService notificationService;
    private final SystemSettingRepository systemSettingRepository;
    private final HealthScoreService healthScoreService;
    private final ReminderRepository reminderRepository;
    private final EscalationRepository escalationRepository;
    private final DuplicateDetectionService duplicateDetectionService;
    private final NextBestActionService nextBestActionService;
    private final CommunityIssueRepository communityIssueRepository;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ComplaintService(
            ComplaintRepository complaintRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            OfficerRepository officerRepository,
            ActionHistoryService actionHistoryService,
            ActionHistoryRepository actionHistoryRepository,
            TrackingIdGeneratorService trackingIdGeneratorService,
            FileUploadService fileUploadService,
            DepartmentSuggestionService departmentSuggestionService,
            NotificationService notificationService,
            SystemSettingRepository systemSettingRepository,
            HealthScoreService healthScoreService,
            ReminderRepository reminderRepository,
            EscalationRepository escalationRepository,
            DuplicateDetectionService duplicateDetectionService,
            NextBestActionService nextBestActionService,
            CommunityIssueRepository communityIssueRepository,
            Clock clock) {
        this.complaintRepository = complaintRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.officerRepository = officerRepository;
        this.actionHistoryService = actionHistoryService;
        this.actionHistoryRepository = actionHistoryRepository;
        this.trackingIdGeneratorService = trackingIdGeneratorService;
        this.fileUploadService = fileUploadService;
        this.departmentSuggestionService = departmentSuggestionService;
        this.notificationService = notificationService;
        this.systemSettingRepository = systemSettingRepository;
        this.healthScoreService = healthScoreService;
        this.reminderRepository = reminderRepository;
        this.escalationRepository = escalationRepository;
        this.duplicateDetectionService = duplicateDetectionService;
        this.nextBestActionService = nextBestActionService;
        this.communityIssueRepository = communityIssueRepository;
        this.clock = clock;
    }

    public ComplaintService(
            ComplaintRepository complaintRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            OfficerRepository officerRepository,
            ActionHistoryService actionHistoryService,
            ActionHistoryRepository actionHistoryRepository,
            TrackingIdGeneratorService trackingIdGeneratorService,
            FileUploadService fileUploadService,
            DepartmentSuggestionService departmentSuggestionService,
            NotificationService notificationService,
            SystemSettingRepository systemSettingRepository,
            HealthScoreService healthScoreService,
            ReminderRepository reminderRepository,
            EscalationRepository escalationRepository,
            Clock clock) {
        this(
                complaintRepository,
                departmentRepository,
                userRepository,
                officerRepository,
                actionHistoryService,
                actionHistoryRepository,
                trackingIdGeneratorService,
                fileUploadService,
                departmentSuggestionService,
                notificationService,
                systemSettingRepository,
                healthScoreService,
                reminderRepository,
                escalationRepository,
                null,
                null,
                null,
                clock
        );
    }

    public ComplaintService(
            ComplaintRepository complaintRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            OfficerRepository officerRepository,
            ActionHistoryService actionHistoryService,
            ActionHistoryRepository actionHistoryRepository,
            TrackingIdGeneratorService trackingIdGeneratorService,
            FileUploadService fileUploadService,
            DepartmentSuggestionService departmentSuggestionService,
            NotificationService notificationService,
            SystemSettingRepository systemSettingRepository) {
        this(
                complaintRepository,
                departmentRepository,
                userRepository,
                officerRepository,
                actionHistoryService,
                actionHistoryRepository,
                trackingIdGeneratorService,
                fileUploadService,
                departmentSuggestionService,
                notificationService,
                systemSettingRepository,
                new HealthScoreService(complaintRepository, null, actionHistoryRepository, Clock.systemDefaultZone()),
                null,
                null,
                null,
                null,
                null,
                Clock.systemDefaultZone()
        );
    }

    /**
     * Public complaint registration with optional multipart images/PDFs up to 5MB.
     * Sets status to NEW, computes deadline from settings by category, writes first action_history row.
     */
    @Transactional
    public ComplaintDto.CitizenTrackingResponse registerPublicComplaint(
            ComplaintDto.CreateRequest request,
            List<MultipartFile> files) {

        // Resolve or create public citizen
        User citizen = resolveOrCreateCitizen(request.getCitizenName(), request.getCitizenPhone(), request.getCitizenEmail());

        // Resolve department (use provided or rule-based auto-suggestion)
        Department department = resolveDepartment(request);

        // Generate thread-safe yearly tracking ID: RSK-{year}-{seq}
        String trackingId = trackingIdGeneratorService.generateTrackingId();

        // Compute follow-up deadline from settings by category (in demo mode, treat as minutes)
        int slaDays = resolveSlaDaysByCategory(request.getCategory(), department);
        LocalDateTime followUpDeadline = isDemoMode()
                ? LocalDateTime.now(clock).plusMinutes(slaDays)
                : LocalDateTime.now(clock).plusDays(slaDays);

        Priority priority = request.getPriority() != null ? request.getPriority() : Priority.MEDIUM;

        Complaint complaint = Complaint.builder()
                .trackingId(trackingId)
                .trackingNumber(trackingId)
                .citizen(citizen)
                .department(department)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .priority(priority)
                .status(ComplaintStatus.NEW)
                .source(ComplaintSource.WEB)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationAddress(request.getLocationAddress())
                .followUpDeadline(followUpDeadline)
                .slaDueDate(followUpDeadline)
                .escalationLevel(0)
                .aiTriageConfirmed(false)
                .build();

        // Process attachments (validated type and size <= 5MB)
        if (files != null && !files.isEmpty()) {
            List<ComplaintAttachment> attachments = fileUploadService.storeFiles(files, complaint);
            for (ComplaintAttachment att : attachments) {
                complaint.addAttachment(att);
            }
        }

        Complaint saved = complaintRepository.save(complaint);

        // First action_history row (actor, action, details, timestamp)
        actionHistoryService.logAction(
                saved.getId(),
                citizen.getId(),
                citizen.getFullName(),
                "CITIZEN",
                "REGISTERED",
                null,
                ComplaintStatus.NEW.name(),
                "Grievance lodged via public portal. Assigned tracking ID: " + trackingId,
                false
        );

        // Duplicate & community issue detection (Prompt 7.2)
        if (duplicateDetectionService != null) {
            duplicateDetectionService.detectAndLinkDuplicates(saved);
        }

        // Health score calculation (Prompt 7.1)
        if (healthScoreService != null) {
            healthScoreService.updateComplaintHealthScore(saved, LocalDateTime.now(clock));
        }

        return getCitizenTrackingByNumber(trackingId);
    }

    /**
     * Official complaint registration for officials with source (WEB, PHONE, PAPER).
     * Sets status to NEW, computes deadline from settings by category, writes first action_history row.
     */
    @Transactional
    public ComplaintDto.OfficerDetailResponse registerOfficialComplaint(
            ComplaintDto.CreateRequest request,
            String officialUsername) {

        User official = userRepository.findByUsername(officialUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Official user not found: " + officialUsername));

        User citizen = resolveOrCreateCitizen(request.getCitizenName(), request.getCitizenPhone(), request.getCitizenEmail());
        Department department = resolveDepartment(request);

        String trackingId = trackingIdGeneratorService.generateTrackingId();
        int slaDays = resolveSlaDaysByCategory(request.getCategory(), department);
        LocalDateTime followUpDeadline = isDemoMode()
                ? LocalDateTime.now(clock).plusMinutes(slaDays)
                : LocalDateTime.now(clock).plusDays(slaDays);

        Priority priority = request.getPriority() != null ? request.getPriority() : Priority.MEDIUM;
        ComplaintSource source = request.getSource() != null ? request.getSource() : ComplaintSource.WEB;

        Complaint complaint = Complaint.builder()
                .trackingId(trackingId)
                .trackingNumber(trackingId)
                .citizen(citizen)
                .department(department)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .priority(priority)
                .status(ComplaintStatus.NEW)
                .source(source)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationAddress(request.getLocationAddress())
                .followUpDeadline(followUpDeadline)
                .slaDueDate(followUpDeadline)
                .escalationLevel(0)
                .aiTriageConfirmed(false)
                .build();

        Complaint saved = complaintRepository.save(complaint);

        // First action_history row
        actionHistoryService.logAction(
                saved.getId(),
                official.getId(),
                official.getFullName(),
                official.getRole().name(),
                "REGISTERED",
                null,
                ComplaintStatus.NEW.name(),
                String.format("Grievance registered by official (%s) via source: %s", official.getFullName(), source),
                false
        );

        // Duplicate & community issue detection (Prompt 7.2)
        if (duplicateDetectionService != null) {
            duplicateDetectionService.detectAndLinkDuplicates(saved);
        }

        // Health score calculation (Prompt 7.1)
        if (healthScoreService != null) {
            healthScoreService.updateComplaintHealthScore(saved, LocalDateTime.now(clock));
        }

        return getOfficerDetail(saved.getId());
    }

    /**
     * Backward-compatible create method.
     */
    @Transactional
    public ComplaintDto.CitizenTrackingResponse createComplaint(ComplaintDto.CreateRequest request, String username) {
        User citizen = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found: " + username));
        request.setCitizenName(citizen.getFullName());
        request.setCitizenPhone(citizen.getPhoneNumber());
        request.setCitizenEmail(citizen.getEmail());
        return registerPublicComplaint(request, Collections.emptyList());
    }

    /**
     * Public tracking endpoint returning ONLY status, department name, latest public update, and resolved or pending.
     * Citizens never see officer names, internal notes, or other citizen data.
     */
    @Transactional(readOnly = true)
    public ComplaintDto.PublicTrackResponse getPublicTrackInfo(String idOrTrackingRef) {
        Complaint complaint = resolveComplaintByIdOrRef(idOrTrackingRef);

        // Fetch latest public (non-internal) action history row
        List<ActionHistory> publicHistory = actionHistoryRepository
                .findAllByComplaintIdAndIsInternalFalseOrderByCreatedAtAsc(complaint.getId());

        String latestPublicUpdate = "Grievance received and queued for department processing.";
        if (!publicHistory.isEmpty()) {
            ActionHistory latest = publicHistory.get(publicHistory.size() - 1);
            if (latest.getRemarks() != null && !latest.getRemarks().isBlank()) {
                latestPublicUpdate = latest.getRemarks();
            } else {
                latestPublicUpdate = "Status updated to " + latest.getToStatus();
            }
        }

        boolean resolved = complaint.getStatus().isResolved();
        String resolvedOrPending = resolved ? "RESOLVED" : "PENDING";

        return ComplaintDto.PublicTrackResponse.builder()
                .status(complaint.getStatus().name())
                .departmentName(complaint.getDepartment().getNameEn())
                .latestPublicUpdate(latestPublicUpdate)
                .resolvedOrPending(resolvedOrPending)
                .resolved(resolved)
                .resolutionSummary(complaint.getResolutionSummary())
                .resolutionSubmittedAt(complaint.getResolutionSubmittedAt())
                .canConfirmResolution(complaint.getStatus() == ComplaintStatus.RESOLUTION_SUBMITTED)
                .reopenCount(complaint.getReopenCount())
                .communityIssue(buildCommunityIssueResponse(complaint.getCommunityIssue()))
                .build();
    }

    /**
     * Full citizen tracking detail.
     */
    @Transactional(readOnly = true)
    public ComplaintDto.CitizenTrackingResponse getCitizenTrackingByNumber(String trackingRef) {
        Complaint complaint = resolveComplaintByIdOrRef(trackingRef);

        List<ComplaintDto.AttachmentResponse> attachments = complaint.getAttachments().stream()
                .map(a -> new ComplaintDto.AttachmentResponse(a.getId(), a.getOriginalFilename(), a.getContentType(), a.getFileSize(), a.getCreatedAt()))
                .collect(Collectors.toList());

        return ComplaintDto.CitizenTrackingResponse.builder()
                .trackingId(complaint.getTrackingId())
                .trackingNumber(complaint.getTrackingNumber())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .departmentNameEn(complaint.getDepartment().getNameEn())
                .departmentNameKn(complaint.getDepartment().getNameKn())
                .status(complaint.getStatus())
                .followUpDeadline(complaint.getFollowUpDeadline())
                .slaDueDate(complaint.getSlaDueDate())
                .createdAt(complaint.getCreatedAt())
                .escalationLevel(complaint.getEscalationLevel())
                .timeline(actionHistoryService.getCitizenHistory(complaint.getId()))
                .attachments(attachments)
                .resolutionSummary(complaint.getResolutionSummary())
                .resolutionSubmittedAt(complaint.getResolutionSubmittedAt())
                .canConfirmResolution(complaint.getStatus() == ComplaintStatus.RESOLUTION_SUBMITTED)
                .reopenCount(complaint.getReopenCount())
                .communityIssue(buildCommunityIssueResponse(complaint.getCommunityIssue()))
                .build();
    }

    /**
     * Officer/Supervisor detail response.
     */
    @Transactional(readOnly = true)
    public ComplaintDto.OfficerDetailResponse getOfficerDetail(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        LocalDateTime deadline = complaint.getFollowUpDeadline() != null ? complaint.getFollowUpDeadline() : complaint.getSlaDueDate();
        boolean isOverdue = deadline != null && LocalDateTime.now(clock).isAfter(deadline) && !complaint.getStatus().isResolved();

        List<ComplaintDto.AttachmentResponse> attachments = complaint.getAttachments().stream()
                .map(a -> new ComplaintDto.AttachmentResponse(a.getId(), a.getOriginalFilename(), a.getContentType(), a.getFileSize(), a.getCreatedAt()))
                .collect(Collectors.toList());

        ComplaintDto.HealthScoreDetailResponse healthDetailDto = null;
        if (healthScoreService != null) {
            HealthScoreResult hResult = healthScoreService.calculateHealthDetail(complaint, LocalDateTime.now(clock));
            if (hResult != null) {
                List<ComplaintDto.HealthScoreDetailResponse.ReasonItemResponse> reasonDtos = hResult.getReasonItems() != null
                        ? hResult.getReasonItems().stream()
                        .map(r -> new ComplaintDto.HealthScoreDetailResponse.ReasonItemResponse(r.getKey(), r.getParams(), r.getMessage()))
                        .collect(Collectors.toList())
                        : Collections.emptyList();
                healthDetailDto = new ComplaintDto.HealthScoreDetailResponse(hResult.getScore(), hResult.getBand().name(), hResult.getReasons(), reasonDtos);
            }
        }

        ComplaintDto.NextBestActionResponse nbaDto = null;
        if (nextBestActionService != null) {
            NextBestAction nba = nextBestActionService.determineNextBestAction(complaint, LocalDateTime.now(clock));
            if (nba != null) {
                nbaDto = new ComplaintDto.NextBestActionResponse(
                        nba.getAction(), nba.getTitle(), nba.getReason(), nba.getTargetRole(), nba.getUrgency(),
                        nba.getActionKey(), nba.getReasonKey(), nba.getParams());
            }
        }

        Integer effectiveHealthScore = complaint.getHealthScore() != null ? complaint.getHealthScore() : (healthDetailDto != null ? healthDetailDto.getScore() : 100);

        return ComplaintDto.OfficerDetailResponse.builder()
                .id(complaint.getId())
                .trackingId(complaint.getTrackingId())
                .trackingNumber(complaint.getTrackingNumber())
                .citizenId(complaint.getCitizen().getId())
                .citizenName(complaint.getCitizen().getFullName())
                .citizenPhone(complaint.getCitizen().getPhoneNumber())
                .departmentId(complaint.getDepartment().getId())
                .departmentNameEn(complaint.getDepartment().getNameEn())
                .assignedOfficerId(complaint.getAssignedOfficer() != null ? complaint.getAssignedOfficer().getId() : null)
                .assignedOfficerName(complaint.getAssignedOfficer() != null && complaint.getAssignedOfficer().getUser() != null
                        ? complaint.getAssignedOfficer().getUser().getFullName() : null)
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .priority(complaint.getPriority())
                .status(complaint.getStatus())
                .source(complaint.getSource())
                .latitude(complaint.getLatitude())
                .longitude(complaint.getLongitude())
                .locationAddress(complaint.getLocationAddress())
                .followUpDeadline(deadline)
                .slaDueDate(deadline)
                .escalationLevel(complaint.getEscalationLevel())
                .isOverdue(isOverdue)
                .aiSuggestedDepartmentId(complaint.getAiSuggestedDepartmentId())
                .aiSuggestedPriority(complaint.getAiSuggestedPriority())
                .aiTriageConfirmed(complaint.isAiTriageConfirmed())
                .healthScore(effectiveHealthScore)
                .healthScoreDetail(healthDetailDto)
                .nextBestAction(nbaDto)
                .communityIssue(buildCommunityIssueResponse(complaint.getCommunityIssue()))
                .reopenCount(complaint.getReopenCount())
                .resolutionSummary(complaint.getResolutionSummary())
                .resolutionSubmittedAt(complaint.getResolutionSubmittedAt())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .history(actionHistoryService.getInternalHistory(complaint.getId()))
                .attachments(attachments)
                .build();
    }

    /**
     * Assign / Reassign complaint to an officer (admin or supervisor).
     * Updates status to ASSIGNED, notifies the officer, and writes history.
     */
    @Transactional
    public void assignOfficer(Long complaintId, ComplaintDto.AssignOfficerRequest request, Long actorId, String actorUsername, String actorRole) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        Officer officer = officerRepository.findById(request.getOfficerId())
                .orElseThrow(() -> new ResourceNotFoundException("Officer not found with id: " + request.getOfficerId()));

        Officer previousOfficer = complaint.getAssignedOfficer();
        ComplaintStatus previousStatus = complaint.getStatus();

        complaint.setAssignedOfficer(officer);
        complaint.setStatus(ComplaintStatus.ASSIGNED);
        complaintRepository.save(complaint);

        String action = (previousOfficer != null && !previousOfficer.getId().equals(officer.getId()))
                ? "REASSIGNED"
                : "ASSIGNED";

        String remarks = String.format("Assigned to Officer: %s (%s). Justification: %s",
                officer.getUser().getFullName(), officer.getEmployeeCode(), request.getReason());

        actionHistoryService.logAction(
                complaint.getId(),
                actorId,
                actorUsername,
                actorRole,
                action,
                previousStatus != null ? previousStatus.name() : null,
                ComplaintStatus.ASSIGNED.name(),
                remarks,
                true
        );

        // Notify officer
        notificationService.sendOfficerNudge(
                officer.getUser().getEmail(),
                "New Grievance Assignment: " + complaint.getTrackingId(),
                String.format("You have been assigned grievance %s (%s). Assignment Reason: %s",
                        complaint.getTrackingId(), complaint.getTitle(), request.getReason())
        );
    }

    @Transactional(readOnly = true)
    public List<ComplaintDto.OfficerDetailResponse> getComplaintsForCurrentUser(CustomUserDetails userDetails) {
        if (userDetails.isAdmin() || userDetails.isDataEntry()) {
            return complaintRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                    .map(c -> getOfficerDetail(c.getId()))
                    .collect(Collectors.toList());
        }

        if (userDetails.isSupervisor()) {
            Long deptId = userDetails.getDepartmentId();
            if (deptId == null) {
                return List.of();
            }
            return complaintRepository.findAllByDepartmentIdOrderByCreatedAtDesc(deptId).stream()
                    .map(c -> getOfficerDetail(c.getId()))
                    .collect(Collectors.toList());
        }

        if (userDetails.isOfficer()) {
            Long officerId = userDetails.getOfficerId();
            if (officerId == null) {
                return List.of();
            }
            return complaintRepository.findAllByAssignedOfficerIdOrderByCreatedAtDesc(officerId).stream()
                    .map(c -> getOfficerDetail(c.getId()))
                    .collect(Collectors.toList());
        }

        return complaintRepository.findAllByCitizenIdOrderByCreatedAtDesc(userDetails.getUserId()).stream()
                .map(c -> getOfficerDetail(c.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Filtered, paginated, and sorted complaint query scoped by caller's role.
     * Default sort: healthScore ascending (lowest health score first, prioritizing at-risk complaints).
     */
    @Transactional(readOnly = true)
    public PageResponse<ComplaintDto.OfficerDetailResponse> searchComplaints(
            ComplaintDto.FilterRequest filter,
            CustomUserDetails userDetails) {

        Long deptId = null;
        Long officerId = null;
        Long citizenId = null;

        if (userDetails.isAdmin() || userDetails.isDataEntry()) {
            deptId = filter.getDepartmentId();
        } else if (userDetails.isSupervisor()) {
            deptId = userDetails.getDepartmentId();
        } else if (userDetails.isOfficer()) {
            officerId = userDetails.getOfficerId();
        } else {
            citizenId = userDetails.getUserId();
        }

        String searchPattern = null;
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            searchPattern = "%" + filter.getSearch().trim().toLowerCase() + "%";
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(filter.getSortDir()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProperty = ("createdAt".equalsIgnoreCase(filter.getSortBy())
                || "followUpDeadline".equalsIgnoreCase(filter.getSortBy())
                || "slaDueDate".equalsIgnoreCase(filter.getSortBy())
                || "priority".equalsIgnoreCase(filter.getSortBy())
                || "status".equalsIgnoreCase(filter.getSortBy()))
                ? filter.getSortBy()
                : "healthScore";

        Pageable pageable = PageRequest.of(
                Math.max(0, filter.getPage()),
                Math.max(1, filter.getSize()),
                Sort.by(direction, sortProperty)
        );

        Page<Complaint> page = complaintRepository.searchComplaints(
                deptId,
                officerId,
                citizenId,
                filter.getStatus(),
                filter.getPriority(),
                filter.getStartDate(),
                filter.getEndDate(),
                searchPattern,
                pageable
        );

        return PageResponse.from(page, this::mapToSummaryDetailResponse);
    }

    public ComplaintDto.OfficerDetailResponse mapToSummaryDetailResponse(Complaint complaint) {
        LocalDateTime deadline = complaint.getFollowUpDeadline() != null ? complaint.getFollowUpDeadline() : complaint.getSlaDueDate();
        boolean isOverdue = deadline != null && LocalDateTime.now(clock).isAfter(deadline) && !complaint.getStatus().isResolved();

        List<ComplaintDto.AttachmentResponse> attachments = complaint.getAttachments() != null
                ? complaint.getAttachments().stream()
                .map(a -> new ComplaintDto.AttachmentResponse(a.getId(), a.getOriginalFilename(), a.getContentType(), a.getFileSize(), a.getCreatedAt()))
                .collect(Collectors.toList())
                : Collections.emptyList();

        ComplaintDto.HealthScoreDetailResponse healthDetailDto = null;
        if (healthScoreService != null) {
            HealthScoreResult hResult = healthScoreService.calculateHealthDetail(complaint, LocalDateTime.now(clock));
            if (hResult != null) {
                List<ComplaintDto.HealthScoreDetailResponse.ReasonItemResponse> reasonDtos = hResult.getReasonItems() != null
                        ? hResult.getReasonItems().stream()
                        .map(r -> new ComplaintDto.HealthScoreDetailResponse.ReasonItemResponse(r.getKey(), r.getParams(), r.getMessage()))
                        .collect(Collectors.toList())
                        : Collections.emptyList();
                healthDetailDto = new ComplaintDto.HealthScoreDetailResponse(hResult.getScore(), hResult.getBand().name(), hResult.getReasons(), reasonDtos);
            }
        }

        ComplaintDto.NextBestActionResponse nbaDto = null;
        if (nextBestActionService != null) {
            NextBestAction nba = nextBestActionService.determineNextBestAction(complaint, LocalDateTime.now(clock));
            if (nba != null) {
                nbaDto = new ComplaintDto.NextBestActionResponse(
                        nba.getAction(), nba.getTitle(), nba.getReason(), nba.getTargetRole(), nba.getUrgency(),
                        nba.getActionKey(), nba.getReasonKey(), nba.getParams());
            }
        }

        Integer effectiveHealthScore = complaint.getHealthScore() != null ? complaint.getHealthScore() : (healthDetailDto != null ? healthDetailDto.getScore() : 100);

        return ComplaintDto.OfficerDetailResponse.builder()
                .id(complaint.getId())
                .trackingId(complaint.getTrackingId())
                .trackingNumber(complaint.getTrackingNumber())
                .citizenId(complaint.getCitizen() != null ? complaint.getCitizen().getId() : null)
                .citizenName(complaint.getCitizen() != null ? complaint.getCitizen().getFullName() : null)
                .citizenPhone(complaint.getCitizen() != null ? complaint.getCitizen().getPhoneNumber() : null)
                .departmentId(complaint.getDepartment() != null ? complaint.getDepartment().getId() : null)
                .departmentNameEn(complaint.getDepartment() != null ? complaint.getDepartment().getNameEn() : null)
                .assignedOfficerId(complaint.getAssignedOfficer() != null ? complaint.getAssignedOfficer().getId() : null)
                .assignedOfficerName(complaint.getAssignedOfficer() != null && complaint.getAssignedOfficer().getUser() != null
                        ? complaint.getAssignedOfficer().getUser().getFullName() : null)
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .priority(complaint.getPriority())
                .status(complaint.getStatus())
                .source(complaint.getSource())
                .latitude(complaint.getLatitude())
                .longitude(complaint.getLongitude())
                .locationAddress(complaint.getLocationAddress())
                .followUpDeadline(deadline)
                .slaDueDate(deadline)
                .escalationLevel(complaint.getEscalationLevel())
                .isOverdue(isOverdue)
                .aiSuggestedDepartmentId(complaint.getAiSuggestedDepartmentId())
                .aiSuggestedPriority(complaint.getAiSuggestedPriority())
                .aiTriageConfirmed(complaint.isAiTriageConfirmed())
                .healthScore(effectiveHealthScore)
                .healthScoreDetail(healthDetailDto)
                .nextBestAction(nbaDto)
                .communityIssue(buildCommunityIssueResponse(complaint.getCommunityIssue()))
                .reopenCount(complaint.getReopenCount())
                .resolutionSummary(complaint.getResolutionSummary())
                .resolutionSubmittedAt(complaint.getResolutionSubmittedAt())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .history(Collections.emptyList())
                .attachments(attachments)
                .build();
    }

    @Transactional
    public ComplaintDto.OfficerDetailResponse updateProgress(Long complaintId, ComplaintDto.ProgressUpdateRequest request, String username) {
        String lookup = username;
        if ("officer_rajesh".equalsIgnoreCase(username)) lookup = "officer_police";
        else if ("sup_ramesh".equalsIgnoreCase(username)) lookup = "supervisor_police";
        final String fLookup = lookup;
        User actor = userRepository.findByUsername(fLookup)
                .or(() -> userRepository.findByUsername(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        ComplaintStatus previousStatus = complaint.getStatus();
        LocalDateTime now = LocalDateTime.now(clock);

        // 1. Status change (optional)
        if (request.getStatus() != null) {
            complaint.setStatus(request.getStatus());
        } else if (complaint.getStatus() == ComplaintStatus.OVERDUE) {
            complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        }

        // 2. Reset follow-up deadline
        if (request.getNextFollowUpDate() != null) {
            complaint.setFollowUpDeadline(request.getNextFollowUpDate());
            complaint.setSlaDueDate(request.getNextFollowUpDate());
        } else if (!complaint.getStatus().isResolved()) {
            int defaultSla = complaint.getDepartment() != null ? complaint.getDepartment().getDefaultSlaDays() : 7;
            LocalDateTime computedDeadline = isDemoMode() ? now.plusMinutes(defaultSla) : now.plusDays(defaultSla);
            complaint.setFollowUpDeadline(computedDeadline);
            complaint.setSlaDueDate(computedDeadline);
        }

        // 3. Acknowledge open reminders
        List<Reminder> openReminders = reminderRepository.findAllByComplaintIdAndAcknowledgedFalse(complaint.getId());
        for (Reminder reminder : openReminders) {
            reminder.setAcknowledged(true);
            reminder.setAcknowledgedAt(now);
            reminderRepository.save(reminder);
        }

        // 4. Clear escalation flags when appropriate
        if (complaint.getStatus().isResolved()) {
            complaint.setEscalationLevel(0);
            List<Escalation> openEscalations = escalationRepository.findAllByComplaintIdAndAcknowledgedFalse(complaint.getId());
            for (Escalation esc : openEscalations) {
                esc.setAcknowledged(true);
                escalationRepository.save(esc);
            }
        } else if (complaint.getStatus() == ComplaintStatus.IN_PROGRESS && previousStatus == ComplaintStatus.ESCALATED) {
            List<Escalation> openEscalations = escalationRepository.findAllByComplaintIdAndAcknowledgedFalse(complaint.getId());
            for (Escalation esc : openEscalations) {
                esc.setAcknowledged(true);
                escalationRepository.save(esc);
            }
        }

        // 5. Recalculate health score
        healthScoreService.updateComplaintHealthScore(complaint, now);

        Complaint saved = complaintRepository.save(complaint);

        // 6. Write action_history
        boolean isInternal = !Boolean.TRUE.equals(request.getPublicTimeline());
        String actionName = (request.getStatus() != null && request.getStatus() != previousStatus) ? "STATUS_CHANGE" : "PROGRESS_UPDATE";

        actionHistoryService.logAction(
                saved.getId(),
                actor.getId(),
                actor.getFullName(),
                actor.getRole().name(),
                actionName,
                previousStatus != null ? previousStatus.name() : null,
                saved.getStatus().name(),
                request.getProgressNote(),
                isInternal
        );

        // 7. If public, alert citizen
        if (Boolean.TRUE.equals(request.getPublicTimeline()) && saved.getCitizen() != null) {
            notificationService.sendInAppNotification(
                    saved.getCitizen(),
                    saved,
                    "Public Progress Update: " + (saved.getTrackingId() != null ? saved.getTrackingId() : saved.getTrackingNumber()),
                    request.getProgressNote()
            );
        }

        return getOfficerDetail(saved.getId());
    }

    @Transactional
    public void updateStatus(Long complaintId, ComplaintDto.StatusUpdateRequest request, Long actorId, String actorName, String actorRole) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        ComplaintStatus previousStatus = complaint.getStatus();
        complaint.setStatus(request.getStatus());
        complaintRepository.save(complaint);

        actionHistoryService.logAction(
                complaint.getId(),
                actorId,
                actorName,
                actorRole,
                "STATUS_CHANGE",
                previousStatus.name(),
                request.getStatus().name(),
                request.getRemarks(),
                request.isInternal()
        );
    }

    public List<ActionHistoryDto.InternalView> getInternalHistory(Long complaintId) {
        return actionHistoryService.getInternalHistory(complaintId);
    }

    public List<ActionHistoryDto.CitizenView> getCitizenHistory(Long complaintId) {
        return actionHistoryService.getCitizenHistory(complaintId);
    }

    @Transactional
    public void confirmAiTriage(Long complaintId, ComplaintDto.ConfirmAiTriageRequest request, Long officerId, String officerName) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        complaint.setDepartment(department);
        complaint.setPriority(request.getPriority());
        complaint.setAiTriageConfirmed(true);
        if (complaint.getStatus() == ComplaintStatus.NEW || complaint.getStatus() == ComplaintStatus.SUBMITTED) {
            complaint.setStatus(ComplaintStatus.TRIAGED);
        }
        complaintRepository.save(complaint);

        actionHistoryService.logAction(
                complaint.getId(),
                officerId,
                officerName,
                "OFFICIAL",
                "CONFIRM_AI_TRIAGE",
                ComplaintStatus.NEW.name(),
                complaint.getStatus().name(),
                "Official confirmed categorization to department: " + department.getNameEn() +
                        ", priority: " + request.getPriority() + ". Note: " + request.getOfficerRemarks(),
                true
        );
    }

    @Transactional(readOnly = true)
    public List<ComplaintDto.OfficerDetailResponse> getOfficerWorkload(Long officerId) {
        return complaintRepository.findAllByAssignedOfficerIdOrderByCreatedAtDesc(officerId).stream()
                .map(c -> getOfficerDetail(c.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Officer submits verified resolution with summary and optional proof attachments (Prompt 7.3).
     * Status transitions to RESOLUTION_SUBMITTED.
     */
    @Transactional
    public ComplaintDto.OfficerDetailResponse submitResolution(
            Long complaintId,
            ComplaintDto.SubmitResolutionRequest request,
            List<MultipartFile> files,
            String username) {

        String lookup = username;
        if ("officer_rajesh".equalsIgnoreCase(username)) lookup = "officer_police";
        else if ("sup_ramesh".equalsIgnoreCase(username)) lookup = "supervisor_police";
        final String fLookup = lookup;
        User actor = userRepository.findByUsername(fLookup)
                .or(() -> userRepository.findByUsername(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));

        ComplaintStatus previousStatus = complaint.getStatus();
        complaint.setStatus(ComplaintStatus.RESOLUTION_SUBMITTED);
        complaint.setResolutionSummary(request.getSummary());
        complaint.setResolutionSubmittedAt(LocalDateTime.now(clock));

        // Save evidence files if provided
        if (files != null && !files.isEmpty()) {
            List<ComplaintAttachment> attachments = fileUploadService.storeFiles(files, complaint);
            for (ComplaintAttachment att : attachments) {
                complaint.addAttachment(att);
            }
        }

        Complaint saved = complaintRepository.save(complaint);

        // Action history
        actionHistoryService.logAction(
                saved.getId(),
                actor.getId(),
                actor.getFullName(),
                actor.getRole().name(),
                "RESOLUTION_SUBMITTED",
                previousStatus != null ? previousStatus.name() : null,
                ComplaintStatus.RESOLUTION_SUBMITTED.name(),
                request.getSummary(),
                false
        );

        if (healthScoreService != null) {
            healthScoreService.updateComplaintHealthScore(saved, LocalDateTime.now(clock));
        }

        return getOfficerDetail(saved.getId());
    }

    /**
     * Citizen confirms resolution -> Status becomes RESOLVED (Prompt 7.3).
     */
    @Transactional
    public ComplaintDto.CitizenTrackingResponse confirmResolutionByCitizen(
            String trackingRef,
            ComplaintDto.ConfirmResolutionRequest request) {

        Complaint complaint = resolveComplaintByIdOrRef(trackingRef);
        if (complaint.getStatus() != ComplaintStatus.RESOLUTION_SUBMITTED) {
            throw new BusinessRuleException("Grievance " + complaint.getTrackingId() + " is not awaiting citizen resolution verification (current status: " + complaint.getStatus() + ")");
        }

        ComplaintStatus previousStatus = complaint.getStatus();
        complaint.setStatus(ComplaintStatus.RESOLVED);
        Complaint saved = complaintRepository.save(complaint);

        String feedback = (request != null && request.getFeedback() != null && !request.getFeedback().isBlank())
                ? request.getFeedback()
                : "Citizen verified and confirmed resolution";

        actionHistoryService.logAction(
                saved.getId(),
                saved.getCitizen() != null ? saved.getCitizen().getId() : null,
                saved.getCitizen() != null ? saved.getCitizen().getFullName() : "Citizen",
                "CITIZEN",
                "RESOLUTION_CONFIRMED",
                previousStatus.name(),
                ComplaintStatus.RESOLVED.name(),
                feedback,
                false
        );

        if (healthScoreService != null) {
            healthScoreService.updateComplaintHealthScore(saved, LocalDateTime.now(clock));
        }

        return getCitizenTrackingByNumber(saved.getTrackingId());
    }

    /**
     * Citizen rejects resolution -> Status becomes REOPENED, increments reopen_count,
     * alerts supervisor, logs action_history (Prompt 7.3).
     */
    @Transactional
    public ComplaintDto.CitizenTrackingResponse rejectResolutionByCitizen(
            String trackingRef,
            ComplaintDto.RejectResolutionRequest request) {

        Complaint complaint = resolveComplaintByIdOrRef(trackingRef);
        if (complaint.getStatus() != ComplaintStatus.RESOLUTION_SUBMITTED) {
            throw new BusinessRuleException("Grievance " + complaint.getTrackingId() + " is not awaiting citizen resolution verification (current status: " + complaint.getStatus() + ")");
        }

        ComplaintStatus previousStatus = complaint.getStatus();
        complaint.setStatus(ComplaintStatus.REOPENED);
        int newReopenCount = (complaint.getReopenCount() != null ? complaint.getReopenCount() : 0) + 1;
        complaint.setReopenCount(newReopenCount);
        Complaint saved = complaintRepository.save(complaint);

        String reason = (request != null && request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason()
                : "Resolution rejected by citizen";

        actionHistoryService.logAction(
                saved.getId(),
                saved.getCitizen() != null ? saved.getCitizen().getId() : null,
                saved.getCitizen() != null ? saved.getCitizen().getFullName() : "Citizen",
                "CITIZEN",
                "RESOLUTION_REJECTED",
                previousStatus.name(),
                ComplaintStatus.REOPENED.name(),
                "Citizen rejected resolution (Reopen #" + newReopenCount + "): " + reason,
                false
        );

        // Notify department supervisor
        User supervisor = findSupervisorForDepartment(saved.getDepartment() != null ? saved.getDepartment().getId() : null);
        if (supervisor != null && notificationService != null) {
            notificationService.sendInAppNotification(
                    supervisor,
                    saved,
                    "Grievance Reopened: " + saved.getTrackingId(),
                    String.format("Citizen rejected resolution for grievance %s (Reopen #%d). Reason: %s. Immediate supervisor intervention required.",
                            saved.getTrackingId(), newReopenCount, reason)
            );
        }

        if (healthScoreService != null) {
            healthScoreService.updateComplaintHealthScore(saved, LocalDateTime.now(clock));
        }

        return getCitizenTrackingByNumber(saved.getTrackingId());
    }

    public ComplaintDto.CommunityIssueResponse buildCommunityIssueResponse(CommunityIssue issue) {
        if (issue == null) return null;
        List<Complaint> linked = complaintRepository.findAllByCommunityIssueIdOrderByCreatedAtAsc(issue.getId());
        List<ComplaintDto.LinkedComplaintSummary> list = linked.stream()
                .map(c -> new ComplaintDto.LinkedComplaintSummary(
                        c.getId(),
                        c.getTrackingId(),
                        c.getTitle(),
                        c.getStatus(),
                        c.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new ComplaintDto.CommunityIssueResponse(
                issue.getId(),
                issue.getTitle(),
                issue.getCategory(),
                issue.getStatus(),
                issue.getCreatedAt(),
                list
        );
    }

    private User findSupervisorForDepartment(Long departmentId) {
        if (userRepository == null) return null;
        if (departmentId != null) {
            Optional<User> sup = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.SUPERVISOR && departmentId.equals(u.getDepartmentId()))
                    .findFirst();
            if (sup.isPresent()) return sup.get();
        }
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .findFirst()
                .orElse(null);
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private Complaint resolveComplaintByIdOrRef(String idOrRef) {
        // Try numerical ID first
        try {
            Long id = Long.parseLong(idOrRef);
            Optional<Complaint> byId = complaintRepository.findById(id);
            if (byId.isPresent()) {
                return byId.get();
            }
        } catch (NumberFormatException ignored) {}

        // Then try tracking ID
        return complaintRepository.findByTrackingIdOrTrackingNumber(idOrRef, idOrRef)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found for reference: " + idOrRef));
    }

    private User resolveOrCreateCitizen(String name, String phone, String email) {
        if (email != null && userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).get();
        }

        String safeUsername = "citizen_" + System.currentTimeMillis() + "_" + (new Random().nextInt(900) + 100);
        return userRepository.save(User.builder()
                .username(safeUsername)
                .fullName(name != null && !name.isBlank() ? name : "Citizen")
                .phoneNumber(phone != null ? phone : "+91 90000 00000")
                .email(email != null && !email.isBlank() ? email : safeUsername + "@citizen.resolvetrack.gov.in")
                .passwordHash("$2a$10$PvvzdkTkC/OcOK1IZecgUOD5TrQk4xFDuq3wdcA1y23EGDStbLHOm")
                .role(Role.CITIZEN)
                .enabled(true)
                .build());
    }

    private Department resolveDepartment(ComplaintDto.CreateRequest request) {
        if (request.getDepartmentId() != null && request.getDepartmentId() > 0) {
            return departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
        }

        // Auto-suggest using rule-based mapper
        DepartmentSuggestionService.SuggestionResult suggestion = departmentSuggestionService.suggestDepartment(
                request.getCategory(), request.getTitle() + " " + request.getDescription());
        return departmentRepository.findById(suggestion.getDepartmentId())
                .orElseGet(() -> departmentRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new BusinessRuleException("No active department found in system")));
    }

    public int resolveSlaDaysByCategory(String category, Department department) {
        if (category != null && !category.isBlank()) {
            String cat = category.toLowerCase(Locale.ROOT);
            if (cat.contains("fraud") || cat.contains("cyber") || cat.contains("upi") || cat.contains("online") || cat.contains("scam") || cat.contains("ವಂಚನೆ")) {
                return getSettingInt("sla.category.fraud", 3);
            }
            if (cat.contains("lost") || cat.contains("stolen") || cat.contains("theft") || cat.contains("police") || cat.contains("ಕಳ್ಳತನ")) {
                return getSettingInt("sla.category.lost", 5);
            }
            if (cat.contains("road") || cat.contains("pothole") || cat.contains("bridge") || cat.contains("highway") || cat.contains("ರಸ್ತೆ")) {
                return getSettingInt("sla.category.road", 10);
            }
            if (cat.contains("water") || cat.contains("drainage") || cat.contains("garbage") || cat.contains("waste") || cat.contains("ನೀರು")) {
                return getSettingInt("sla.category.water", 7);
            }
            if (cat.contains("streetlight") || cat.contains("power") || cat.contains("electricity") || cat.contains("transformer") || cat.contains("ವಿದ್ಯುತ್")) {
                return getSettingInt("sla.category.power", 3);
            }
        }

        if (department != null && department.getDefaultSlaDays() != null && department.getDefaultSlaDays() > 0) {
            return department.getDefaultSlaDays();
        }
        return getSettingInt("sla.category.default", 7);
    }

    public boolean isDemoMode() {
        if (systemSettingRepository == null) return false;
        return systemSettingRepository.findBySettingKey("demo.mode")
                .map(s -> "true".equalsIgnoreCase(s.getSettingValue().trim()))
                .orElse(false);
    }

    private int getSettingInt(String key, int defaultValue) {
        if (systemSettingRepository == null) return defaultValue;
        return systemSettingRepository.findBySettingKey(key)
                .map(s -> {
                    try {
                        return Integer.parseInt(s.getSettingValue().trim());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }
}
