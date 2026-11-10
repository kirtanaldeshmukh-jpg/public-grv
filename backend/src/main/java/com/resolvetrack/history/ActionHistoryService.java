package com.resolvetrack.history;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActionHistoryService {

    private final ActionHistoryRepository actionHistoryRepository;

    public ActionHistoryService(ActionHistoryRepository actionHistoryRepository) {
        this.actionHistoryRepository = actionHistoryRepository;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public ActionHistory logAction(
            Long complaintId,
            Long actorId,
            String actorName,
            String actorRole,
            String action,
            String fromStatus,
            String toStatus,
            String remarks,
            boolean isInternal) {

        ActionHistory history = ActionHistory.builder()
                .complaintId(complaintId)
                .actorId(actorId)
                .actorName(actorName)
                .actorRole(actorRole)
                .action(action)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .remarks(remarks)
                .isInternal(isInternal)
                .build();

        return actionHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<ActionHistoryDto.InternalView> getAllInternalHistory() {
        return actionHistoryRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")).stream()
                .map(h -> ActionHistoryDto.InternalView.builder()
                        .id(h.getId())
                        .complaintId(h.getComplaintId())
                        .actorId(h.getActorId())
                        .actorName(h.getActorName())
                        .actorRole(h.getActorRole())
                        .action(h.getAction())
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .remarks(h.getRemarks())
                        .isInternal(h.isInternal())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActionHistoryDto.InternalView> getInternalHistory(Long complaintId) {
        return actionHistoryRepository.findAllByComplaintIdOrderByCreatedAtAsc(complaintId).stream()
                .map(h -> ActionHistoryDto.InternalView.builder()
                        .id(h.getId())
                        .complaintId(h.getComplaintId())
                        .actorId(h.getActorId())
                        .actorName(h.getActorName())
                        .actorRole(h.getActorRole())
                        .action(h.getAction())
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .remarks(h.getRemarks())
                        .isInternal(h.isInternal())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActionHistoryDto.CitizenView> getCitizenHistory(Long complaintId) {
        return actionHistoryRepository.findAllByComplaintIdAndIsInternalFalseOrderByCreatedAtAsc(complaintId).stream()
                .map(h -> ActionHistoryDto.CitizenView.builder()
                        .id(h.getId())
                        .action(h.getAction())
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .publicRemarks(h.getRemarks())
                        .timestamp(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
