package com.resolvetrack.history;

import com.resolvetrack.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = {"/api/history", "/api/v1/history"})
public class ActionHistoryController {

    private final ActionHistoryService actionHistoryService;

    public ActionHistoryController(ActionHistoryService actionHistoryService) {
        this.actionHistoryService = actionHistoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OFFICER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ActionHistoryDto.InternalView>>> getAllInternalHistory() {
        return ResponseEntity.ok(ApiResponse.ok(actionHistoryService.getAllInternalHistory()));
    }

    @GetMapping("/complaint/{complaintId}/internal")
    @PreAuthorize("hasAnyRole('OFFICER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ActionHistoryDto.InternalView>>> getInternalAuditTrail(@PathVariable Long complaintId) {
        return ResponseEntity.ok(ApiResponse.ok(actionHistoryService.getInternalHistory(complaintId)));
    }

    @GetMapping("/complaint/{complaintId}/citizen")
    public ResponseEntity<ApiResponse<List<ActionHistoryDto.CitizenView>>> getCitizenAuditTimeline(@PathVariable Long complaintId) {
        return ResponseEntity.ok(ApiResponse.ok(actionHistoryService.getCitizenHistory(complaintId)));
    }
}
