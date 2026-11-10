package com.resolvetrack.complaint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Optional<Complaint> findByTrackingId(String trackingId);

    Optional<Complaint> findByTrackingNumber(String trackingNumber);

    Optional<Complaint> findByTrackingIdOrTrackingNumber(String trackingId, String trackingNumber);

    default boolean existsByTrackingIdOrTrackingNumber(String trackingId, String trackingNumber) {
        return findByTrackingId(trackingId).isPresent()
                || (trackingNumber != null && findByTrackingNumber(trackingNumber).isPresent());
    }

    List<Complaint> findAllByCitizenIdOrderByCreatedAtDesc(Long citizenId);

    List<Complaint> findAllByDepartmentIdOrderByCreatedAtDesc(Long departmentId);

    List<Complaint> findAllByAssignedOfficerIdOrderByCreatedAtDesc(Long assignedOfficerId);

    List<Complaint> findAllByStatus(ComplaintStatus status);

    List<Complaint> findAllByDepartmentIdAndStatus(Long departmentId, ComplaintStatus status);

    // For reminder engine: complaints nearing follow_up_deadline / SLA not yet resolved or rejected
    @Query("SELECT c FROM Complaint c WHERE c.status NOT IN ('RESOLVED', 'REJECTED') AND c.followUpDeadline <= :threshold AND c.followUpDeadline > :now")
    List<Complaint> findComplaintsNearingDeadline(@Param("threshold") LocalDateTime threshold, @Param("now") LocalDateTime now);

    // For escalation engine: complaints past follow_up_deadline not resolved or rejected and escalationLevel < :maxLevel
    @Query("SELECT c FROM Complaint c WHERE c.status NOT IN ('RESOLVED', 'REJECTED') AND c.followUpDeadline < :now AND c.escalationLevel < :maxLevel")
    List<Complaint> findOverdueForEscalation(@Param("now") LocalDateTime now, @Param("maxLevel") int maxLevel);

    long countByStatus(ComplaintStatus status);

    long countByDepartmentId(Long departmentId);

    long countByAssignedOfficerId(Long assignedOfficerId);

    @Query(value = "SELECT c FROM Complaint c " +
            "WHERE (:deptId IS NULL OR c.department.id = :deptId) " +
            "AND (:officerId IS NULL OR (c.assignedOfficer IS NOT NULL AND c.assignedOfficer.id = :officerId)) " +
            "AND (:citizenId IS NULL OR (c.citizen IS NOT NULL AND c.citizen.id = :citizenId)) " +
            "AND (:status IS NULL OR c.status = :status) " +
            "AND (:priority IS NULL OR c.priority = :priority) " +
            "AND (cast(:startDate as java.time.LocalDateTime) IS NULL OR c.createdAt >= :startDate) " +
            "AND (cast(:endDate as java.time.LocalDateTime) IS NULL OR c.createdAt <= :endDate) " +
            "AND (:search IS NULL OR LOWER(c.title) LIKE :search " +
            "     OR LOWER(c.description) LIKE :search " +
            "     OR LOWER(c.category) LIKE :search " +
            "     OR LOWER(c.trackingId) LIKE :search " +
            "     OR (c.trackingNumber IS NOT NULL AND LOWER(c.trackingNumber) LIKE :search) " +
            "     OR (c.citizen IS NOT NULL AND LOWER(c.citizen.fullName) LIKE :search))",
            countQuery = "SELECT COUNT(c) FROM Complaint c " +
            "WHERE (:deptId IS NULL OR c.department.id = :deptId) " +
            "AND (:officerId IS NULL OR (c.assignedOfficer IS NOT NULL AND c.assignedOfficer.id = :officerId)) " +
            "AND (:citizenId IS NULL OR (c.citizen IS NOT NULL AND c.citizen.id = :citizenId)) " +
            "AND (:status IS NULL OR c.status = :status) " +
            "AND (:priority IS NULL OR c.priority = :priority) " +
            "AND (cast(:startDate as java.time.LocalDateTime) IS NULL OR c.createdAt >= :startDate) " +
            "AND (cast(:endDate as java.time.LocalDateTime) IS NULL OR c.createdAt <= :endDate) " +
            "AND (:search IS NULL OR LOWER(c.title) LIKE :search " +
            "     OR LOWER(c.description) LIKE :search " +
            "     OR LOWER(c.category) LIKE :search " +
            "     OR LOWER(c.trackingId) LIKE :search " +
            "     OR (c.trackingNumber IS NOT NULL AND LOWER(c.trackingNumber) LIKE :search) " +
            "     OR (c.citizen IS NOT NULL AND LOWER(c.citizen.fullName) LIKE :search))")
    Page<Complaint> searchComplaints(
            @Param("deptId") Long deptId,
            @Param("officerId") Long officerId,
            @Param("citizenId") Long citizenId,
            @Param("status") ComplaintStatus status,
            @Param("priority") Priority priority,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> countByStatusAll();

    @Query("SELECT c.status, COUNT(c) FROM Complaint c WHERE c.department.id = :deptId GROUP BY c.status")
    List<Object[]> countByStatusForDepartment(@Param("deptId") Long deptId);

    @Query("SELECT c.status, COUNT(c) FROM Complaint c WHERE c.assignedOfficer.id = :officerId GROUP BY c.status")
    List<Object[]> countByStatusForOfficer(@Param("officerId") Long officerId);

    @Query("SELECT c.status, COUNT(c) FROM Complaint c WHERE c.citizen.id = :citizenId GROUP BY c.status")
    List<Object[]> countByStatusForCitizen(@Param("citizenId") Long citizenId);

    @Query("SELECT c FROM Complaint c WHERE c.department.id = :deptId AND LOWER(c.category) = LOWER(:category) AND c.status NOT IN ('RESOLVED', 'REJECTED') AND c.createdAt >= :since ORDER BY c.createdAt DESC")
    List<Complaint> findOpenByDepartmentAndCategorySince(@Param("deptId") Long deptId, @Param("category") String category, @Param("since") LocalDateTime since);

    List<Complaint> findAllByCommunityIssueIdOrderByCreatedAtAsc(Long communityIssueId);

    @Query("SELECT c FROM Complaint c WHERE c.status = 'RESOLUTION_SUBMITTED' AND c.resolutionSubmittedAt <= :cutoff")
    List<Complaint> findAwaitingResolutionReviewBefore(@Param("cutoff") LocalDateTime cutoff);
}
