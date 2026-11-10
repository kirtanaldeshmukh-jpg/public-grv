package com.resolvetrack.escalation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EscalationRepository extends JpaRepository<Escalation, Long> {
    List<Escalation> findAllByComplaintIdOrderByEscalatedAtDesc(Long complaintId);
    List<Escalation> findAllByNotifiedSupervisorIdOrderByEscalatedAtDesc(Long supervisorId);
    List<Escalation> findAllByComplaintIdAndAcknowledgedFalse(Long complaintId);
    boolean existsByComplaintIdAndToLevel(Long complaintId, Integer toLevel);
    long countByAcknowledgedFalse();
}
