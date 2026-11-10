package com.resolvetrack.admin;

import com.resolvetrack.common.ApiResponse;
import com.resolvetrack.complaint.DataSeederService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(path = {"/api/admin", "/api/v1/admin"})
public class AdminController {

    private final DataSeederService dataSeederService;

    public AdminController(DataSeederService dataSeederService) {
        this.dataSeederService = dataSeederService;
    }

    /**
     * Admin-only endpoint to wipe and reseed 25 realistic sample grievances across Police and Civic categories.
     * Resets tracking sequence to 25 so the next registered complaint is RSK-{year}-026.
     */
    @PostMapping(path = {"/reset-and-reseed", "/reseed-demo-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetAndReseedData() {
        Map<String, Object> result = dataSeederService.resetAndReseed();
        return ResponseEntity.ok(ApiResponse.ok("Database reset and 25 sample grievances reseeded successfully", result));
    }
}

