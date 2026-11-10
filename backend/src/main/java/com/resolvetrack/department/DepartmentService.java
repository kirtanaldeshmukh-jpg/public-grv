package com.resolvetrack.department;

import com.resolvetrack.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto.Response> getActiveDepartments() {
        return departmentRepository.findAllByActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentDto.Response getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    public DepartmentDto.Response mapToDto(Department d) {
        return DepartmentDto.Response.builder()
                .id(d.getId())
                .code(d.getCode())
                .nameEn(d.getNameEn())
                .nameKn(d.getNameKn())
                .description(d.getDescription())
                .defaultSlaDays(d.getDefaultSlaDays())
                .escalationHours(d.getEscalationHours())
                .active(d.isActive())
                .build();
    }
}
