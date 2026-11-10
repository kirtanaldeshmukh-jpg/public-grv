package com.resolvetrack.escalation;

import com.resolvetrack.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = {"/api/escalations", "/api/v1/escalations"})
public class EscalationController {

    private final EscalationService escalationService;

    public EscalationController(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OFFICER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<EscalationDto.Response>>> getAllEscalations() {
        return ResponseEntity.ok(ApiResponse.ok(escalationService.getAllEscalations()));
    }

    @GetMapping("/complaint/{complaintId}")
    @PreAuthorize("hasAnyRole('OFFICER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<EscalationDto.Response>>> getEscalationsByComplaint(@PathVariable Long complaintId) {
        return ResponseEntity.ok(ApiResponse.ok(escalationService.getEscalationHistory(complaintId)));
    }

    @PutMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> acknowledgeEscalation(@PathVariable Long id) {
        escalationService.acknowledgeEscalation(id);
        return ResponseEntity.ok(ApiResponse.ok("Escalation alert acknowledged by supervisor", null));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> acknowledgeEscalationPost(@PathVariable Long id) {
        return acknowledgeEscalation(id);
    }

    @PostMapping("/trigger-scan")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> triggerManualScan() {
        int escalated = escalationService.processStatutoryEscalations();
        return ResponseEntity.ok(ApiResponse.ok("Manual escalation scan completed", escalated));
    }
}
