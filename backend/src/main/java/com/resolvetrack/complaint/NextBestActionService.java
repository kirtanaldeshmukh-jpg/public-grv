package com.resolvetrack.complaint;

import com.resolvetrack.auth.CustomUserDetails;
import com.resolvetrack.common.SystemSettingRepository;
import com.resolvetrack.history.ActionHistory;
import com.resolvetrack.history.ActionHistoryRepository;
import com.resolvetrack.officer.Officer;
import com.resolvetrack.officer.OfficerRepository;
import com.resolvetrack.reminder.ReminderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class NextBestActionService {

    private static final Pattern MISSING_DOC_PATTERN = Pattern.compile(
            ".*\\b(document|documents|proof|receipt|bill|ration|aadhar|aadhaar|fir|copy|attachment|missing|ದಾಖಲೆ|ಪುರಾವೆ|ರಸೀದಿ)\\b.*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private final ComplaintRepository complaintRepository;
    private final ActionHistoryRepository actionHistoryRepository;
    private final ReminderRepository reminderRepository;
    private final SystemSettingRepository systemSettingRepository;
    @SuppressWarnings("unused")
    private final OfficerRepository officerRepository;
    private final Clock clock;

    public NextBestActionService(
            ComplaintRepository complaintRepository,
            ActionHistoryRepository actionHistoryRepository,
            ReminderRepository reminderRepository,
            SystemSettingRepository systemSettingRepository,
            OfficerRepository officerRepository,
            Clock clock) {
        this.complaintRepository = complaintRepository;
        this.actionHistoryRepository = actionHistoryRepository;
        this.reminderRepository = reminderRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.officerRepository = officerRepository;
        this.clock = clock;
    }

    public NextBestAction determineNextBestAction(Complaint complaint) {
        return determineNextBestAction(complaint, LocalDateTime.now(clock));
    }

    /**
     * Deterministic rule engine evaluating the single highest-priority recommended next action:
     * 1. resolution submitted -> request citizen verification
     * 2. unassigned -> assign officer
     * 3. repeated delays / disputes -> escalate
     * 4. overdue and no update -> notify supervisor
     * 5. deadline within lead time -> send reminder
     * 6. missing documents -> request documents
     * 7. fallback in progress -> record progress
     */
    public NextBestAction determineNextBestAction(Complaint complaint, LocalDateTime now) {
        if (complaint == null) {
            return NextBestAction.of("NONE", "No Grievance", "No active grievance provided", "SYSTEM", "LOW");
        }

        ComplaintStatus status = complaint.getStatus();
        if (status == ComplaintStatus.RESOLVED) {
            return NextBestAction.of("ARCHIVE", "Grievance Closed", "Grievance resolved and verified by citizen", "SYSTEM", "LOW");
        }
        if (status == ComplaintStatus.REJECTED) {
            return NextBestAction.of("ARCHIVE", "Grievance Dismissed", "Grievance has been dismissed", "SYSTEM", "LOW");
        }

        // Rule 1: Resolution submitted -> request citizen verification
        if (status == ComplaintStatus.RESOLUTION_SUBMITTED) {
            return NextBestAction.of(
                    "REQUEST_CITIZEN_VERIFICATION",
                    "Awaiting Citizen Verification",
                    "Resolution submitted: Request citizen sign-off",
                    "CITIZEN",
                    "HIGH",
                    "nba.action.REQUEST_CITIZEN_VERIFICATION",
                    "nba.reason.resolution_submitted",
                    Map.of()
            );
        }

        // Rule 2: Unassigned -> assign officer
        if (complaint.getAssignedOfficer() == null || status == ComplaintStatus.NEW) {
            return NextBestAction.of(
                    "ASSIGN_OFFICER",
                    "Assign Field Officer",
                    "Grievance is unassigned: Allocate to jurisdictional field officer",
                    "SUPERVISOR",
                    "CRITICAL",
                    "nba.action.ASSIGN_OFFICER",
                    "nba.reason.unassigned",
                    Map.of()
            );
        }

        LocalDateTime deadline = complaint.getFollowUpDeadline() != null
                ? complaint.getFollowUpDeadline()
                : complaint.getSlaDueDate();
        boolean isOverdue = deadline != null && now.isAfter(deadline);

        // Rule 3: Repeated delays or disputes -> escalate
        int reopenCount = complaint.getReopenCount() != null ? complaint.getReopenCount() : 0;
        int escalationLevel = complaint.getEscalationLevel() != null ? complaint.getEscalationLevel() : 0;
        long unackedReminders = complaint.getId() != null
                ? reminderRepository.countByComplaintIdAndAcknowledgedFalse(complaint.getId())
                : 0;

        if (reopenCount > 0 || escalationLevel >= 2 || (isOverdue && unackedReminders >= 2)) {
            Map<String, Object> params = new HashMap<>();
            params.put("reopenCount", reopenCount);
            params.put("escalationLevel", escalationLevel);
            params.put("unackedReminders", unackedReminders);
            return NextBestAction.of(
                    "ESCALATE",
                    "Escalate Grievance",
                    "Repeated delays or citizen dispute: Escalate docket to higher authority",
                    "SUPERVISOR",
                    "CRITICAL",
                    "nba.action.ESCALATE",
                    "nba.reason.delays_or_dispute",
                    params
            );
        }

        // Rule 4: Overdue and no update -> notify supervisor
        if (isOverdue) {
            boolean hasRecentUpdate = false;
            if (complaint.getId() != null) {
                List<ActionHistory> histories = actionHistoryRepository
                        .findAllByComplaintIdOrderByCreatedAtAsc(complaint.getId());
                if (!histories.isEmpty()) {
                    ActionHistory latest = histories.get(histories.size() - 1);
                    if (latest.getCreatedAt() != null && latest.getCreatedAt().isAfter(now.minusHours(24))) {
                        hasRecentUpdate = true;
                    }
                }
            }
            if (!hasRecentUpdate) {
                return NextBestAction.of(
                        "NOTIFY_SUPERVISOR",
                        "Alert Department Supervisor",
                        "Overdue without recent progress notes: Alert departmental supervisor",
                        "OFFICER",
                        "CRITICAL",
                        "nba.action.NOTIFY_SUPERVISOR",
                        "nba.reason.overdue_no_update",
                        Map.of()
                );
            }
        }

        // Rule 5: Deadline within lead time -> send reminder
        if (deadline != null && !now.isAfter(deadline)) {
            long leadHours = getReminderLeadTimeHours();
            Duration remaining = Duration.between(now, deadline);
            if (!remaining.isNegative() && remaining.toHours() <= leadHours) {
                return NextBestAction.of(
                        "SEND_REMINDER",
                        "Dispatch SLA Reminder",
                        "SLA deadline approaching within lead window: Dispatch urgent reminder",
                        "OFFICER",
                        "HIGH",
                        "nba.action.SEND_REMINDER",
                        "nba.reason.deadline_approaching",
                        Map.of("leadHours", leadHours)
                );
            }
        }

        // Rule 6: Missing documents -> request documents
        boolean hasNoAttachments = complaint.getAttachments() == null || complaint.getAttachments().isEmpty();
        String textToAnalyze = (complaint.getTitle() != null ? complaint.getTitle() : "") + " "
                + (complaint.getDescription() != null ? complaint.getDescription() : "");
        if (hasNoAttachments && MISSING_DOC_PATTERN.matcher(textToAnalyze).matches()) {
            return NextBestAction.of(
                    "REQUEST_DOCUMENTS",
                    "Request Citizen Documents",
                    "Missing documents or evidence: Request supplementary proofs from citizen",
                    "CITIZEN",
                    "MEDIUM",
                    "nba.action.REQUEST_DOCUMENTS",
                    "nba.reason.missing_documents",
                    Map.of()
            );
        }

        // Fallback: active on-track complaint
        return NextBestAction.of(
                "RECORD_PROGRESS",
                "Log Investigation Update",
                "Complaint is progressing within SLA: Continue field investigation and log updates",
                "OFFICER",
                "MEDIUM",
                "nba.action.RECORD_PROGRESS",
                "nba.reason.active_investigation",
                Map.of()
        );
    }

    private long getReminderLeadTimeHours() {
        return systemSettingRepository.findBySettingKey("reminder.lead_time")
                .map(s -> {
                    try {
                        return Long.parseLong(s.getSettingValue().trim());
                    } catch (NumberFormatException e) {
                        return 24L;
                    }
                })
                .orElse(24L);
    }

    @Transactional(readOnly = true)
    public RecommendedActionsSummaryDto getRecommendedActionsSummary(CustomUserDetails userDetails) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Complaint> activeComplaints = complaintRepository.findAll().stream()
                .filter(c -> c.getStatus() != null && !c.getStatus().isResolved())
                .collect(Collectors.toList());

        // Scope filter based on caller's role
        if (userDetails.isSupervisor()) {
            Long deptId = userDetails.getDepartmentId();
            activeComplaints = activeComplaints.stream()
                    .filter(c -> c.getDepartment() != null && Objects.equals(c.getDepartment().getId(), deptId))
                    .collect(Collectors.toList());
        } else if (userDetails.isOfficer()) {
            Long officerId = userDetails.getOfficerId();
            activeComplaints = activeComplaints.stream()
                    .filter(c -> c.getAssignedOfficer() != null && Objects.equals(c.getAssignedOfficer().getId(), officerId))
                    .collect(Collectors.toList());
        }

        int totalOverdue = 0;
        int totalApproachingDeadline = 0;
        int totalUnassigned = 0;
        int totalVerificationPending = 0;

        Map<Long, OfficerGroupBuilder> groupsByOfficer = new LinkedHashMap<>();

        for (Complaint complaint : activeComplaints) {
            NextBestAction nba = determineNextBestAction(complaint, now);
            LocalDateTime deadline = complaint.getFollowUpDeadline() != null
                    ? complaint.getFollowUpDeadline() : complaint.getSlaDueDate();
            boolean isOverdue = deadline != null && now.isAfter(deadline);
            boolean isApproaching = deadline != null && !isOverdue && Duration.between(now, deadline).toHours() <= 24;

            if (isOverdue) totalOverdue++;
            if (isApproaching) totalApproachingDeadline++;
            if (complaint.getAssignedOfficer() == null) totalUnassigned++;
            if (complaint.getStatus() == ComplaintStatus.RESOLUTION_SUBMITTED) totalVerificationPending++;

            Officer officer = complaint.getAssignedOfficer();
            Long officerKey = officer != null ? officer.getId() : 0L;
            String officerName = officer != null && officer.getUser() != null ? officer.getUser().getFullName() : "Unassigned Queue";
            String departmentName = complaint.getDepartment() != null ? complaint.getDepartment().getNameEn() : "General";

            OfficerGroupBuilder group = groupsByOfficer.computeIfAbsent(officerKey, k -> new OfficerGroupBuilder(k, officerName, departmentName));
            if (isOverdue) group.overdueCount++;
            if (isApproaching) group.approachingDeadlineCount++;

            group.actions.add(new ActionItemDto(
                    complaint.getId(),
                    complaint.getTrackingId(),
                    complaint.getTitle(),
                    complaint.getCategory(),
                    complaint.getPriority(),
                    complaint.getStatus(),
                    deadline,
                    nba
            ));
        }

        List<OfficerRecommendedGroupDto> groups = groupsByOfficer.values().stream()
                .map(OfficerGroupBuilder::toDto)
                .collect(Collectors.toList());

        return new RecommendedActionsSummaryDto(
                totalOverdue,
                totalApproachingDeadline,
                totalUnassigned,
                totalVerificationPending,
                groups
        );
    }

    private static class OfficerGroupBuilder {
        private final Long officerId;
        private final String officerName;
        private final String departmentName;
        private int overdueCount = 0;
        private int approachingDeadlineCount = 0;
        private final List<ActionItemDto> actions = new ArrayList<>();

        public OfficerGroupBuilder(Long officerId, String officerName, String departmentName) {
            this.officerId = officerId;
            this.officerName = officerName;
            this.departmentName = departmentName;
        }

        public OfficerRecommendedGroupDto toDto() {
            return new OfficerRecommendedGroupDto(officerId, officerName, departmentName, overdueCount, approachingDeadlineCount, actions);
        }
    }

    public record RecommendedActionsSummaryDto(
            int totalOverdue,
            int totalApproachingDeadline,
            int totalUnassigned,
            int totalVerificationPending,
            List<OfficerRecommendedGroupDto> officerGroups
    ) {}

    public record OfficerRecommendedGroupDto(
            Long officerId,
            String officerName,
            String departmentName,
            int overdueCount,
            int approachingDeadlineCount,
            List<ActionItemDto> actions
    ) {}

    public record ActionItemDto(
            Long complaintId,
            String trackingId,
            String title,
            String category,
            Priority priority,
            ComplaintStatus status,
            LocalDateTime deadline,
            NextBestAction nextBestAction
    ) {}
}

