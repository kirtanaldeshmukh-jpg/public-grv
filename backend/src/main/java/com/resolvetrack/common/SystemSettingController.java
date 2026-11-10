package com.resolvetrack.common;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = {"/api/settings", "/api/v1/settings"})
public class SystemSettingController {

    private final SystemSettingRepository systemSettingRepository;

    public SystemSettingController(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemSetting>>> getAllSettings() {
        return ResponseEntity.ok(ApiResponse.ok("System settings retrieved", systemSettingRepository.findAll()));
    }

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<SystemSetting>> getSettingByKey(@PathVariable String key) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with key: " + key));
        return ResponseEntity.ok(ApiResponse.ok(setting));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemSetting>> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.getOrDefault("settingValue", body.get("value"));
        String desc = body.getOrDefault("description", "");

        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseGet(() -> new SystemSetting(key, value != null ? value : "", desc));

        if (value != null) {
            setting.setSettingValue(value);
        }
        if (body.containsKey("description")) {
            setting.setDescription(desc);
        }

        SystemSetting saved = systemSettingRepository.save(setting);
        return ResponseEntity.ok(ApiResponse.ok("Setting updated successfully", saved));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemSetting>> createOrUpdateSetting(@RequestBody Map<String, String> body) {
        String key = body.get("settingKey");
        if (key == null) key = body.get("key");
        if (key == null || key.isBlank()) {
            throw new BusinessRuleException("settingKey is required");
        }
        return updateSetting(key, body);
    }
}

