package com.resolvetrack.reminder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findAllByOfficerIdOrderBySentAtDesc(Long officerId);
    List<Reminder> findAllByComplaintIdOrderBySentAtDesc(Long complaintId);
    List<Reminder> findAllByComplaintIdAndAcknowledgedFalse(Long complaintId);
    boolean existsByComplaintIdAndReminderTypeAndAcknowledgedFalse(Long complaintId, String reminderType);
    boolean existsByComplaintIdAndReminderTypeAndSentAtAfter(Long complaintId, String reminderType, LocalDateTime after);
    long countByComplaintIdAndAcknowledgedFalse(Long complaintId);
    long countByOfficerIdAndAcknowledgedFalse(Long officerId);
    Optional<Reminder> findTopByComplaintIdAndReminderTypeOrderBySentAtDesc(Long complaintId, String reminderType);
}
