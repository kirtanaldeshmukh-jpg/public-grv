package com.resolvetrack.complaint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityIssueRepository extends JpaRepository<CommunityIssue, Long> {

    List<CommunityIssue> findByDepartmentIdAndStatus(Long departmentId, String status);

    @Query("SELECT ci FROM CommunityIssue ci LEFT JOIN FETCH ci.complaints WHERE ci.id = :id")
    CommunityIssue findByIdWithComplaints(@Param("id") Long id);
}

