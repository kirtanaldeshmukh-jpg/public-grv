package com.resolvetrack.complaint;

import com.resolvetrack.common.SystemSetting;
import com.resolvetrack.common.SystemSettingRepository;
import com.resolvetrack.department.Department;
import com.resolvetrack.history.ActionHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DuplicateDetectionServiceTest {

    private ComplaintRepository complaintRepository;
    private CommunityIssueRepository communityIssueRepository;
    private SystemSettingRepository systemSettingRepository;
    private ActionHistoryService actionHistoryService;
    private Clock fixedClock;
    private LocalDateTime fixedNow;

    private DuplicateDetectionService duplicateDetectionService;

    @BeforeEach
    void setUp() {
        complaintRepository = mock(ComplaintRepository.class);
        communityIssueRepository = mock(CommunityIssueRepository.class);
        systemSettingRepository = mock(SystemSettingRepository.class);
        actionHistoryService = mock(ActionHistoryService.class);

        fixedNow = LocalDateTime.of(2026, 11, 8, 11, 0, 0);
        fixedClock = Clock.fixed(fixedNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

        when(systemSettingRepository.findBySettingKey("duplicate.jaccard.threshold"))
                .thenReturn(Optional.of(new SystemSetting(1L, "duplicate.jaccard.threshold", "0.35", "Threshold", fixedNow)));
        when(systemSettingRepository.findBySettingKey("duplicate.radius.meters"))
                .thenReturn(Optional.of(new SystemSetting(2L, "duplicate.radius.meters", "500", "Radius", fixedNow)));

        duplicateDetectionService = new DuplicateDetectionService(
                complaintRepository, communityIssueRepository, systemSettingRepository,
                actionHistoryService, fixedClock);
    }

    @Test
    @DisplayName("Prompt 7.2: Tokenization and Jaccard similarity across English and Kannada tokens")
    void testTokenizationAndJaccard() {
        String text1 = "Huge pothole on 100 feet road near Indiranagar metro station";
        String text2 = "Dangerous pothole near 100 feet road Indiranagar metro station";

        double sim = duplicateDetectionService.calculateJaccardSimilarity(text1, text2);
        assertTrue(sim > 0.5, "Expected high Jaccard similarity for similar pothole complaints, got: " + sim);

        String kn1 = "ಇಂದಿರಾನಗರ ರಸ್ತೆ ಗುಂಡಿ ದುರಸ್ತಿ ಮಾಡಿ dayavittu";
        String kn2 = "ಇಂದಿರಾನಗರ ರಸ್ತೆ ದೊಡ್ಡ ಗುಂಡಿ ide samasyi";
        double knSim = duplicateDetectionService.calculateJaccardSimilarity(kn1, kn2);
        assertTrue(knSim > 0.3, "Expected Kannada token overlap, got: " + knSim);
    }

    @Test
    @DisplayName("Prompt 7.2: Haversine distance correctly identifies proximity within 500 meters")
    void testHaversineDistance() {
        // Point A: Indiranagar 100ft road (12.9783, 77.6408)
        // Point B: 200m away (12.9790, 77.6415)
        // Point C: 2km away in Koramangala (12.9352, 77.6245)
        double distClose = duplicateDetectionService.calculateHaversineDistance(12.9783, 77.6408, 12.9790, 77.6415);
        assertTrue(distClose < 300, "Expected distance < 300m, was: " + distClose);

        double distFar = duplicateDetectionService.calculateHaversineDistance(12.9783, 77.6408, 12.9352, 77.6245);
        assertTrue(distFar > 1500, "Expected distance > 1.5km, was: " + distFar);
    }

    @Test
    @DisplayName("Prompt 7.2: Duplicate detected creates CommunityIssue and preserves individual tracking IDs")
    void testDuplicateDetectionCreatesCommunityIssueAndPreservesTrackingIds() {
        Department pwd = Department.builder().id(3L).nameEn("Public Works").build();

        Complaint existing = Complaint.builder()
                .id(100L)
                .trackingId("RSK-2026-100")
                .category("Roads & Potholes")
                .department(pwd)
                .title("Deep pothole near Indiranagar metro")
                .description("Dangerous deep pothole on main road causing accidents")
                .locationAddress("100 Feet Rd, Indiranagar, Bengaluru")
                .latitude(new BigDecimal("12.97830000"))
                .longitude(new BigDecimal("77.64080000"))
                .status(ComplaintStatus.IN_PROGRESS)
                .build();

        when(complaintRepository.findOpenByDepartmentAndCategorySince(eq(3L), eq("Roads & Potholes"), any()))
                .thenReturn(List.of(existing));

        when(communityIssueRepository.save(any(CommunityIssue.class))).thenAnswer(invocation -> {
            CommunityIssue issue = invocation.getArgument(0);
            issue.setId(501L);
            return issue;
        });

        Complaint newComplaint = Complaint.builder()
                .id(101L)
                .trackingId("RSK-2026-101")
                .category("Roads & Potholes")
                .department(pwd)
                .title("Deep pothole near Indiranagar metro station")
                .description("Dangerous deep pothole on road causing accidents and hazard")
                .locationAddress("100 Feet Rd, Indiranagar, Bengaluru")
                .latitude(new BigDecimal("12.97850000"))
                .longitude(new BigDecimal("77.64100000"))
                .status(ComplaintStatus.NEW)
                .build();

        Optional<CommunityIssue> result = duplicateDetectionService.detectAndLinkDuplicates(newComplaint);

        assertTrue(result.isPresent());
        CommunityIssue issue = result.get();
        assertEquals(501L, issue.getId());
        assertEquals("Roads & Potholes", issue.getCategory());

        // Individual tracking IDs preserved on both complaints
        assertEquals("RSK-2026-100", existing.getTrackingId());
        assertEquals("RSK-2026-101", newComplaint.getTrackingId());

        // Both linked to the community issue
        assertNotNull(newComplaint.getCommunityIssue());
        assertEquals(501L, newComplaint.getCommunityIssue().getId());
        assertNotNull(existing.getCommunityIssue());
        assertEquals(501L, existing.getCommunityIssue().getId());

        // Verify history logged for both
        verify(actionHistoryService, times(1)).logAction(
                eq(100L), any(), any(), eq("SYSTEM"), eq("COMMUNITY_ISSUE_LINKED"),
                any(), any(), contains("RSK-2026-101"), eq(false)
        );
        verify(actionHistoryService, times(1)).logAction(
                eq(101L), any(), any(), eq("SYSTEM"), eq("COMMUNITY_ISSUE_LINKED"),
                any(), any(), contains("RSK-2026-100"), eq(false)
        );
    }

    @Test
    @DisplayName("Prompt 7.2: Subsequent complaint links to existing CommunityIssue")
    void testSubsequentComplaintExtendsCommunityIssue() {
        Department pwd = Department.builder().id(3L).nameEn("Public Works").build();
        CommunityIssue existingIssue = CommunityIssue.builder()
                .id(777L)
                .title("Community Issue: Roads & Potholes at Indiranagar")
                .category("Roads & Potholes")
                .department(pwd)
                .status("OPEN")
                .build();

        Complaint existing = Complaint.builder()
                .id(100L)
                .trackingId("RSK-2026-100")
                .category("Roads & Potholes")
                .department(pwd)
                .title("Deep pothole near Indiranagar metro")
                .description("Dangerous deep pothole on main road")
                .locationAddress("100 Feet Rd, Indiranagar")
                .latitude(new BigDecimal("12.97830000"))
                .longitude(new BigDecimal("77.64080000"))
                .status(ComplaintStatus.IN_PROGRESS)
                .communityIssue(existingIssue)
                .build();

        when(complaintRepository.findOpenByDepartmentAndCategorySince(eq(3L), eq("Roads & Potholes"), any()))
                .thenReturn(List.of(existing));

        Complaint thirdComplaint = Complaint.builder()
                .id(102L)
                .trackingId("RSK-2026-102")
                .category("Roads & Potholes")
                .department(pwd)
                .title("Pothole problem near Indiranagar metro")
                .description("Dangerous pothole causing severe traffic")
                .locationAddress("100 Feet Rd, Indiranagar")
                .latitude(new BigDecimal("12.97840000"))
                .longitude(new BigDecimal("77.64090000"))
                .status(ComplaintStatus.NEW)
                .build();

        Optional<CommunityIssue> result = duplicateDetectionService.detectAndLinkDuplicates(thirdComplaint);

        assertTrue(result.isPresent());
        assertEquals(777L, result.get().getId());
        assertEquals(777L, thirdComplaint.getCommunityIssue().getId());
        verify(communityIssueRepository, never()).save(any()); // Existing issue extended, no new row created
    }
}
