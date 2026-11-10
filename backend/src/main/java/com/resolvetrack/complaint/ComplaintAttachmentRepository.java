package com.resolvetrack.complaint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintAttachmentRepository extends JpaRepository<ComplaintAttachment, Long> {
    List<ComplaintAttachment> findAllByComplaintIdOrderByCreatedAtAsc(Long complaintId);
}

