package com.resolvetrack.officer;

import com.resolvetrack.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = {"/api/officers", "/api/v1/officers"})
public class OfficerController {

    private final OfficerService officerService;

    public OfficerController(OfficerService officerService) {
        this.officerService = officerService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OFFICER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OfficerDto.Response>>> getAllOfficers() {
        return ResponseEntity.ok(ApiResponse.ok(officerService.getAllOfficers()));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('OFFICER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OfficerDto.Response>>> getOfficersByDepartment(@PathVariable Long departmentId) {
        return ResponseEntity.ok(ApiResponse.ok(officerService.getOfficersByDepartment(departmentId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OFFICER', 'SUPERVISOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<OfficerDto.Response>> getOfficerById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(officerService.getOfficerById(id)));
    }
}
