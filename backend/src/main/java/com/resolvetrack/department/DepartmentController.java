package com.resolvetrack.department;

import com.resolvetrack.common.ApiResponse;
import com.resolvetrack.officer.OfficerDto;
import com.resolvetrack.officer.OfficerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = {"/api/departments", "/api/v1/departments"})
public class DepartmentController {

    private final DepartmentService departmentService;
    private final OfficerService officerService;

    public DepartmentController(DepartmentService departmentService, OfficerService officerService) {
        this.departmentService = departmentService;
        this.officerService = officerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentDto.Response>>> getAllDepartments() {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.getActiveDepartments()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDto.Response>> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.getDepartmentById(id)));
    }

    @GetMapping("/{id}/officers")
    public ResponseEntity<ApiResponse<List<OfficerDto.Response>>> getDepartmentOfficers(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(officerService.getOfficersByDepartment(id)));
    }
}
