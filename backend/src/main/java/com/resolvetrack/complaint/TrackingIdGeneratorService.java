package com.resolvetrack.complaint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class TrackingIdGeneratorService {

    private final TrackingSequenceRepository sequenceRepository;
    private final ComplaintRepository complaintRepository;

    @Autowired
    public TrackingIdGeneratorService(
            TrackingSequenceRepository sequenceRepository,
            @Autowired(required = false) ComplaintRepository complaintRepository) {
        this.sequenceRepository = sequenceRepository;
        this.complaintRepository = complaintRepository;
    }

    public TrackingIdGeneratorService(TrackingSequenceRepository sequenceRepository) {
        this(sequenceRepository, null);
    }

    /**
     * Generates a thread-safe, yearly resetting tracking ID in the format:
     * RSK-{year}-{3 digit sequence} (e.g. RSK-2026-001, RSK-2026-002).
     */
    @Transactional
    public synchronized String generateTrackingId() {
        int currentYear = LocalDate.now().getYear();
        return generateTrackingIdForYear(currentYear);
    }

    @Transactional
    public synchronized String generateTrackingIdForYear(int year) {
        TrackingSequence seq = sequenceRepository.findByYearForUpdate(year)
                .orElseGet(() -> {
                    TrackingSequence newSeq = new TrackingSequence();
                    newSeq.setYear(year);
                    newSeq.setCurrentSequence(0L);
                    return sequenceRepository.saveAndFlush(newSeq);
                });

        long nextSequence = seq.getCurrentSequence() + 1;
        String candidate = String.format("RSK-%d-%03d", year, nextSequence);
        if (complaintRepository != null) {
            while (complaintRepository.existsByTrackingIdOrTrackingNumber(candidate, candidate)) {
                nextSequence++;
                candidate = String.format("RSK-%d-%03d", year, nextSequence);
            }
        }
        seq.setCurrentSequence(nextSequence);
        sequenceRepository.saveAndFlush(seq);

        return candidate;
    }
}
