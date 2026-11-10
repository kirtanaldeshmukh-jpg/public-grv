package com.resolvetrack.analytics;

import com.resolvetrack.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"/api/analytics", "/api/v1/analytics"})
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('OFFICER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<AnalyticsDto.ExecutiveOverview>> getExecutiveOverview() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getExecutiveOverview()));
    }
}
