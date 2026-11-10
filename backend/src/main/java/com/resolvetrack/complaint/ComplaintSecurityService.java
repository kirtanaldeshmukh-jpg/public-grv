package com.resolvetrack.complaint;

import com.resolvetrack.auth.CustomUserDetails;
import com.resolvetrack.auth.Role;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("complaintSecurity")
public class ComplaintSecurityService {

    private final ComplaintRepository complaintRepository;

    public ComplaintSecurityService(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    public boolean canAccessComplaint(Authentication authentication, Long complaintId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return false;
        }

        if (userDetails.isAdmin() || userDetails.isDataEntry()) {
            return true;
        }

        Optional<Complaint> complaintOpt = complaintRepository.findById(complaintId);
        if (complaintOpt.isEmpty()) {
            return true; // Let controller handle 404 ResourceNotFoundException
        }

        Complaint complaint = complaintOpt.get();

        if (userDetails.isSupervisor()) {
            return complaint.getDepartment() != null
                    && complaint.getDepartment().getId() != null
                    && complaint.getDepartment().getId().equals(userDetails.getDepartmentId());
        }

        if (userDetails.isOfficer()) {
            return complaint.getAssignedOfficer() != null
                    && complaint.getAssignedOfficer().getId() != null
                    && complaint.getAssignedOfficer().getId().equals(userDetails.getOfficerId());
        }

        if (userDetails.getRole() == Role.CITIZEN) {
            return complaint.getCitizen() != null
                    && complaint.getCitizen().getId() != null
                    && complaint.getCitizen().getId().equals(userDetails.getUserId());
        }

        return false;
    }

    public boolean canUpdateComplaint(Authentication authentication, Long complaintId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return false;
        }

        if (userDetails.isAdmin()) {
            return true;
        }

        Optional<Complaint> complaintOpt = complaintRepository.findById(complaintId);
        if (complaintOpt.isEmpty()) {
            return true;
        }

        Complaint complaint = complaintOpt.get();

        if (userDetails.isSupervisor()) {
            return complaint.getDepartment() != null
                    && complaint.getDepartment().getId() != null
                    && complaint.getDepartment().getId().equals(userDetails.getDepartmentId());
        }

        if (userDetails.isOfficer()) {
            return complaint.getAssignedOfficer() != null
                    && complaint.getAssignedOfficer().getId() != null
                    && complaint.getAssignedOfficer().getId().equals(userDetails.getOfficerId());
        }

        return false;
    }

    public boolean canAccessDepartment(Authentication authentication, Long departmentId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return false;
        }

        if (userDetails.isAdmin() || userDetails.isDataEntry()) {
            return true;
        }

        if (userDetails.isSupervisor() || userDetails.isOfficer()) {
            return departmentId != null && departmentId.equals(userDetails.getDepartmentId());
        }

        return false;
    }
}

