package com.resolvetrack.analytics;

import com.resolvetrack.auth.CustomUserDetails;
import com.resolvetrack.common.ApiResponse;
import com.resolvetrack.complaint.NextBestActionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"/api/dashboard", "/api/v1/dashboard"})
public class DashboardController {

    private final DashboardService dashboardService;
    private final NextBestActionService nextBestActionService;

    @org.springframework.beans.factory.annotation.Autowired
    public DashboardController(DashboardService dashboardService, NextBestActionService nextBestActionService) {
        this.dashboardService = dashboardService;
        this.nextBestActionService = nextBestActionService;
    }

    public DashboardController(DashboardService dashboardService) {
        this(dashboardService, null);
    }

    /**
     * Role-scoped dashboard summary counts:
     * - total, new, assigned, in progress, pending, overdue and resolved.
     * - ADMIN & DATA_ENTRY: unscoped across all departments.
     * - SUPERVISOR: scoped to supervisor's department.
     * - OFFICER: scoped to officer's assigned grievances.
     * - CITIZEN: scoped to citizen's filed grievances.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'OFFICER', 'DATA_ENTRY', 'CITIZEN')")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboardSummary(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        DashboardSummaryDto summary = dashboardService.getSummary(userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Dashboard summary retrieved successfully", summary));
    }

    /**
     * Recommended Next Actions grouped per officer and urgency (Prompt 7.1).
     */
    @GetMapping("/recommended-actions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'OFFICER', 'DATA_ENTRY')")
    public ResponseEntity<ApiResponse<NextBestActionService.RecommendedActionsSummaryDto>> getRecommendedActions(
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        NextBestActionService.RecommendedActionsSummaryDto actions = nextBestActionService.getRecommendedActionsSummary(userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Recommended actions retrieved successfully", actions));
    }

    /**
     * Aggregated Analytics (Prompt 8.1):
     * Average resolution time, overdue percentage, pending, escalation count, reopened count,
     * officer workload, overdue by department, and overdue by area.
     */
    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'OFFICER', 'DATA_ENTRY')")
    public ResponseEntity<ApiResponse<AnalyticsDashboardDto>> getAnalytics(Authentication authentication) {
        CustomUserDetails userDetails = authentication != null && authentication.getPrincipal() instanceof CustomUserDetails ud
                ? ud
                : null;
        AnalyticsDashboardDto analytics = dashboardService.getAnalytics(userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Aggregated analytics retrieved successfully", analytics));
    }

    /**
     * Supervisor Daily Morning Briefing (Prompt 8.1):
     * Generates a template-driven daily briefing narrative and flags top priority case.
     */
    @GetMapping("/briefing")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'OFFICER', 'DATA_ENTRY')")
    public ResponseEntity<ApiResponse<SupervisorBriefingDto>> getSupervisorBriefing(Authentication authentication) {
        CustomUserDetails userDetails = authentication != null && authentication.getPrincipal() instanceof CustomUserDetails ud
                ? ud
                : null;
        SupervisorBriefingDto briefing = dashboardService.getSupervisorBriefing(userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Daily morning briefing generated successfully", briefing));
    }
}
