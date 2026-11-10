package com.resolvetrack.reminder;

import com.resolvetrack.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = {"/api/reminders", "/api/v1/reminders"})
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OFFICER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ReminderDto.Response>>> getAllReminders() {
        return ResponseEntity.ok(ApiResponse.ok(reminderService.getAllReminders()));
    }

    @GetMapping("/officer/{officerId}")
    @PreAuthorize("hasAnyRole('OFFICER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ReminderDto.Response>>> getOfficerReminders(@PathVariable Long officerId) {
        return ResponseEntity.ok(ApiResponse.ok(reminderService.getOfficerReminders(officerId)));
    }

    @PutMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('OFFICER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> acknowledgeReminder(@PathVariable Long id) {
        reminderService.acknowledgeReminder(id);
        return ResponseEntity.ok(ApiResponse.ok("Reminder acknowledged", null));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('OFFICER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> acknowledgeReminderPost(@PathVariable Long id) {
        return acknowledgeReminder(id);
    }

    @PostMapping("/trigger-manual-nudge")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> triggerManualNudges() {
        int count = reminderService.dispatchPreDeadlineNudges();
        return ResponseEntity.ok(ApiResponse.ok("Manual nudge trigger completed", count));
    }
}
