package com.resolvetrack.officer;

import com.resolvetrack.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OfficerService {

    private final OfficerRepository officerRepository;

    public OfficerService(OfficerRepository officerRepository) {
        this.officerRepository = officerRepository;
    }

    @Transactional(readOnly = true)
    public List<OfficerDto.Response> getAllOfficers() {
        return officerRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OfficerDto.Response> getOfficersByDepartment(Long departmentId) {
        return officerRepository.findAllByDepartmentId(departmentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OfficerDto.Response getOfficerById(Long id) {
        return officerRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Officer not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public OfficerDto.Response getOfficerByUserId(Long userId) {
        return officerRepository.findByUserId(userId)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Officer record not found for user id: " + userId));
    }

    public OfficerDto.Response mapToDto(Officer o) {
        return OfficerDto.Response.builder()
                .id(o.getId())
                .userId(o.getUser().getId())
                .fullName(o.getUser().getFullName())
                .email(o.getUser().getEmail())
                .employeeCode(o.getEmployeeCode())
                .departmentId(o.getDepartment().getId())
                .departmentNameEn(o.getDepartment().getNameEn())
                .departmentNameKn(o.getDepartment().getNameKn())
                .designation(o.getDesignation())
                .jurisdictionZone(o.getJurisdictionZone())
                .activeComplaintsCount(o.getActiveComplaintsCount())
                .build();
    }
}
