package com.resolvetrack.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionHistoryRepository extends JpaRepository<ActionHistory, Long> {
    List<ActionHistory> findAllByComplaintIdOrderByCreatedAtAsc(Long complaintId);
    List<ActionHistory> findAllByComplaintIdAndIsInternalFalseOrderByCreatedAtAsc(Long complaintId);
}

