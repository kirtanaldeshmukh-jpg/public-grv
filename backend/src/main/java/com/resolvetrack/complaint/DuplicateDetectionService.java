package com.resolvetrack.complaint;

import com.resolvetrack.common.SystemSettingRepository;
import com.resolvetrack.history.ActionHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DuplicateDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DuplicateDetectionService.class);

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "in", "to", "of", "and", "is", "for", "on", "with", "at", "by", "from",
            "up", "about", "into", "over", "after", "this", "that", "these", "those", "my", "our",
            "near", "please", "kindly", "urgently", "sir", "madam", "issue", "problem", "complaint",
            "there", "here", "road", "street", "area", "ward",
            // Kannada words / transliterations
            "mattuu", "elli", "alli", "illa", "ide", "idu", "adu", "nanna", "namma",
            "krupaya", "dayavittu", "samasyi", "beku", "madidare", "agide"
    );

    private static final Pattern CLEAN_PATTERN = Pattern.compile("[^\\p{L}\\p{Nd}\\s]+");

    private final ComplaintRepository complaintRepository;
    private final CommunityIssueRepository communityIssueRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final ActionHistoryService actionHistoryService;
    private final Clock clock;

    public DuplicateDetectionService(
            ComplaintRepository complaintRepository,
            CommunityIssueRepository communityIssueRepository,
            SystemSettingRepository systemSettingRepository,
            ActionHistoryService actionHistoryService,
            Clock clock) {
        this.complaintRepository = complaintRepository;
        this.communityIssueRepository = communityIssueRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.actionHistoryService = actionHistoryService;
        this.clock = clock;
    }

    /**
     * Checks if a newly registered complaint matches any open complaint in the same department
     * and category within 500m (or same area) in the last 30 days with token Jaccard similarity.
     * If matched, links to a CommunityIssue without deleting or merging either complaint.
     */
    @Transactional
    public Optional<CommunityIssue> detectAndLinkDuplicates(Complaint newComplaint) {
        if (newComplaint == null || newComplaint.getDepartment() == null || newComplaint.getCategory() == null) {
            return Optional.empty();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        List<Complaint> candidates = complaintRepository.findOpenByDepartmentAndCategorySince(
                newComplaint.getDepartment().getId(),
                newComplaint.getCategory(),
                thirtyDaysAgo
        );

        double jaccardThreshold = getJaccardThreshold();
        double radiusMeters = getRadiusMeters();

        Complaint bestMatch = null;
        double bestSimilarity = 0.0;

        for (Complaint candidate : candidates) {
            // Do not match against self
            if (newComplaint.getId() != null && newComplaint.getId().equals(candidate.getId())) {
                continue;
            }

            boolean spatialMatch = isSpatialMatch(newComplaint, candidate, radiusMeters);
            if (!spatialMatch) {
                continue;
            }

            double similarity = calculateJaccardSimilarity(
                    (newComplaint.getTitle() + " " + newComplaint.getDescription()),
                    (candidate.getTitle() + " " + candidate.getDescription())
            );

            if (similarity >= jaccardThreshold && similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = candidate;
            }
        }

        if (bestMatch == null) {
            return Optional.empty();
        }

        log.info("Duplicate cluster detected between complaint {} and candidate {} (similarity={})",
                newComplaint.getTrackingId(), bestMatch.getTrackingId(), bestSimilarity);

        CommunityIssue communityIssue;
        if (bestMatch.getCommunityIssue() != null) {
            // Extend existing community issue
            communityIssue = bestMatch.getCommunityIssue();
            newComplaint.setCommunityIssue(communityIssue);
            complaintRepository.save(newComplaint);

            actionHistoryService.logAction(
                    newComplaint.getId(),
                    null,
                    "ResolveTrack AI Duplicate Engine",
                    "SYSTEM",
                    "COMMUNITY_ISSUE_LINKED",
                    null,
                    newComplaint.getStatus().name(),
                    String.format("Linked to Community Issue #%d ('%s') alongside complaint %s (Similarity: %.0f%%)",
                            communityIssue.getId(), communityIssue.getTitle(), bestMatch.getTrackingId(), bestSimilarity * 100),
                    false
            );
        } else {
            // Create a new CommunityIssue cluster
            String location = newComplaint.getLocationAddress() != null ? newComplaint.getLocationAddress()
                    : (bestMatch.getLocationAddress() != null ? bestMatch.getLocationAddress() : "Localized Area");
            String title = String.format("Community Issue: %s at %s", newComplaint.getCategory(), location);

            communityIssue = CommunityIssue.builder()
                    .title(title)
                    .category(newComplaint.getCategory())
                    .department(newComplaint.getDepartment())
                    .latitude(newComplaint.getLatitude() != null ? newComplaint.getLatitude() : bestMatch.getLatitude())
                    .longitude(newComplaint.getLongitude() != null ? newComplaint.getLongitude() : bestMatch.getLongitude())
                    .locationAddress(location)
                    .status("OPEN")
                    .build();

            communityIssue = communityIssueRepository.save(communityIssue);

            // Link existing match
            bestMatch.setCommunityIssue(communityIssue);
            complaintRepository.save(bestMatch);
            actionHistoryService.logAction(
                    bestMatch.getId(),
                    null,
                    "ResolveTrack AI Duplicate Engine",
                    "SYSTEM",
                    "COMMUNITY_ISSUE_LINKED",
                    null,
                    bestMatch.getStatus().name(),
                    String.format("Linked to Community Issue #%d ('%s') alongside new complaint %s (Similarity: %.0f%%)",
                            communityIssue.getId(), communityIssue.getTitle(), newComplaint.getTrackingId(), bestSimilarity * 100),
                    false
            );

            // Link new complaint
            newComplaint.setCommunityIssue(communityIssue);
            complaintRepository.save(newComplaint);
            actionHistoryService.logAction(
                    newComplaint.getId(),
                    null,
                    "ResolveTrack AI Duplicate Engine",
                    "SYSTEM",
                    "COMMUNITY_ISSUE_LINKED",
                    null,
                    newComplaint.getStatus().name(),
                    String.format("Cluster match detected: Linked to Community Issue #%d ('%s') alongside complaint %s (Similarity: %.0f%%)",
                            communityIssue.getId(), communityIssue.getTitle(), bestMatch.getTrackingId(), bestSimilarity * 100),
                    false
            );
        }

        return Optional.of(communityIssue);
    }

    public boolean isSpatialMatch(Complaint c1, Complaint c2, double radiusMeters) {
        BigDecimal lat1 = c1.getLatitude();
        BigDecimal lon1 = c1.getLongitude();
        BigDecimal lat2 = c2.getLatitude();
        BigDecimal lon2 = c2.getLongitude();

        // 1. Precise GPS Haversine match
        if (lat1 != null && lon1 != null && lat2 != null && lon2 != null) {
            double distance = calculateHaversineDistance(
                    lat1.doubleValue(), lon1.doubleValue(),
                    lat2.doubleValue(), lon2.doubleValue()
            );
            return distance <= radiusMeters;
        }

        // 2. Area / Address text substring match fallback
        if (c1.getLocationAddress() != null && c2.getLocationAddress() != null) {
            Set<String> tokens1 = tokenize(c1.getLocationAddress());
            Set<String> tokens2 = tokenize(c2.getLocationAddress());
            Set<String> intersection = new HashSet<>(tokens1);
            intersection.retainAll(tokens2);
            return !intersection.isEmpty();
        }

        return false;
    }

    public double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371000; // meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    public double calculateJaccardSimilarity(String text1, String text2) {
        Set<String> set1 = tokenize(text1);
        Set<String> set2 = tokenize(text2);

        if (set1.isEmpty() && set2.isEmpty()) return 1.0;
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    public Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }

        String cleaned = CLEAN_PATTERN.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ");
        String[] words = cleaned.trim().split("\\s+");

        return Arrays.stream(words)
                .map(String::trim)
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());
    }

    private double getJaccardThreshold() {
        return systemSettingRepository.findBySettingKey("duplicate.jaccard.threshold")
                .map(s -> {
                    try {
                        return Double.parseDouble(s.getSettingValue().trim());
                    } catch (NumberFormatException e) {
                        return 0.35;
                    }
                })
                .orElse(0.35);
    }

    private double getRadiusMeters() {
        return systemSettingRepository.findBySettingKey("duplicate.radius.meters")
                .map(s -> {
                    try {
                        return Double.parseDouble(s.getSettingValue().trim());
                    } catch (NumberFormatException e) {
                        return 500.0;
                    }
                })
                .orElse(500.0);
    }
}

