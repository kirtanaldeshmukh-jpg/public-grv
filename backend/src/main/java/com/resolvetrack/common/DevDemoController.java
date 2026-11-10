package com.resolvetrack.common;

import com.resolvetrack.reminder.FollowUpScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path = {"/api/dev/demo", "/api/v1/dev/demo"})
public class DevDemoController {

    private final FollowUpScheduler followUpScheduler;
    private final SystemSettingRepository systemSettingRepository;
    private final com.resolvetrack.complaint.DataSeederService dataSeederService;

    public DevDemoController(
            FollowUpScheduler followUpScheduler,
            SystemSettingRepository systemSettingRepository,
            com.resolvetrack.complaint.DataSeederService dataSeederService) {
        this.followUpScheduler = followUpScheduler;
        this.systemSettingRepository = systemSettingRepository;
        this.dataSeederService = dataSeederService;
    }

    /**
     * Reseed demo dockets on demand.
     */
    @PostMapping(path = {"/reset-seed", "/reseed"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetSeed() {
        Map<String, Object> result = dataSeederService.resetAndReseed();
        return ResponseEntity.ok(ApiResponse.ok("Demo data reseeded successfully with 25 dockets", result));
    }

    /**
     * Trigger manual follow-up cycle execution (reminders, overdue transition, escalations, health score recalculation).
     */
    @PostMapping("/run-cycle")
    public ResponseEntity<ApiResponse<FollowUpScheduler.CycleResult>> runCycle() {
        FollowUpScheduler.CycleResult result = followUpScheduler.runCycle();
        return ResponseEntity.ok(ApiResponse.ok("Follow-up cycle executed successfully", result));
    }

    /**
     * Configure demo mode (in demo mode, all intervals are minutes instead of hours/days).
     */
    @PostMapping("/set-mode")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setDemoMode(@RequestParam(defaultValue = "true") boolean enabled) {
        SystemSetting setting = systemSettingRepository.findBySettingKey("demo.mode")
                .orElseGet(() -> new SystemSetting("demo.mode", "false", "Demo mode flag (minutes instead of hours)"));
        setting.setSettingValue(String.valueOf(enabled));
        systemSettingRepository.save(setting);

        return ResponseEntity.ok(ApiResponse.ok("Demo mode updated", Map.of(
                "demoMode", enabled,
                "unit", enabled ? "minutes" : "hours"
        )));
    }

    /**
     * Get current demo mode and cycle status.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDemoStatus() {
        boolean demoMode = systemSettingRepository.findBySettingKey("demo.mode")
                .map(s -> "true".equalsIgnoreCase(s.getSettingValue().trim()))
                .orElse(false);

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "demoMode", demoMode,
                "leadTimeHoursOrMinutes", systemSettingRepository.findBySettingKey("reminder.lead_time").map(SystemSetting::getSettingValue).orElse("24"),
                "escalationDelayHoursOrMinutes", systemSettingRepository.findBySettingKey("escalation.delay").map(SystemSetting::getSettingValue).orElse("48")
        )));
    }
}

