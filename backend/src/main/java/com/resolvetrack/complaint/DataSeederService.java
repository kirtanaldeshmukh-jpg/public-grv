package com.resolvetrack.complaint;

import com.resolvetrack.auth.Role;
import com.resolvetrack.auth.User;
import com.resolvetrack.auth.UserRepository;
import com.resolvetrack.common.SystemSettingRepository;
import com.resolvetrack.department.Department;
import com.resolvetrack.department.DepartmentRepository;
import com.resolvetrack.escalation.Escalation;
import com.resolvetrack.escalation.EscalationRepository;
import com.resolvetrack.history.ActionHistory;
import com.resolvetrack.history.ActionHistoryRepository;
import com.resolvetrack.notification.NotificationRepository;
import com.resolvetrack.officer.Officer;
import com.resolvetrack.officer.OfficerRepository;
import com.resolvetrack.reminder.ReminderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataSeederService {

    private static final Logger log = LoggerFactory.getLogger(DataSeederService.class);

    private final ComplaintRepository complaintRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final OfficerRepository officerRepository;
    private final ActionHistoryRepository actionHistoryRepository;
    private final ReminderRepository reminderRepository;
    private final EscalationRepository escalationRepository;
    private final NotificationRepository notificationRepository;
    private final CommunityIssueRepository communityIssueRepository;
    private final TrackingSequenceRepository trackingSequenceRepository;
    private final ComplaintAttachmentRepository attachmentRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final Clock clock;

    public DataSeederService(
            ComplaintRepository complaintRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            OfficerRepository officerRepository,
            ActionHistoryRepository actionHistoryRepository,
            ReminderRepository reminderRepository,
            EscalationRepository escalationRepository,
            NotificationRepository notificationRepository,
            CommunityIssueRepository communityIssueRepository,
            TrackingSequenceRepository trackingSequenceRepository,
            ComplaintAttachmentRepository attachmentRepository,
            SystemSettingRepository systemSettingRepository,
            Clock clock) {
        this.complaintRepository = complaintRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.officerRepository = officerRepository;
        this.actionHistoryRepository = actionHistoryRepository;
        this.reminderRepository = reminderRepository;
        this.escalationRepository = escalationRepository;
        this.notificationRepository = notificationRepository;
        this.communityIssueRepository = communityIssueRepository;
        this.trackingSequenceRepository = trackingSequenceRepository;
        this.attachmentRepository = attachmentRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.clock = clock;
    }

    /**
     * Seed if complaints table is currently empty on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedIfEmpty() {
        try {
            long count = complaintRepository.count();
            if (count == 0) {
                log.info("Complaints database is empty. Auto-seeding 25 realistic sample dockets...");
                resetAndReseed();
            } else {
                log.info("Complaints database already contains {} records. Skipping startup seed.", count);
            }
        } catch (Exception e) {
            log.warn("Could not check/run startup seeder: {}", e.getMessage());
        }
    }

    /**
     * Resets all complaints, history, reminders, and escalations, then seeds 25 realistic sample complaints.
     */
    @Transactional
    public Map<String, Object> resetAndReseed() {
        LocalDateTime now = LocalDateTime.now(clock);
        log.info("Resetting and reseeding grievance records as of {}", now);

        // 1. Clean existing dependent transactional data in batch and flush immediately
        notificationRepository.deleteAllInBatch();
        reminderRepository.deleteAllInBatch();
        escalationRepository.deleteAllInBatch();
        actionHistoryRepository.deleteAllInBatch();
        attachmentRepository.deleteAllInBatch();

        // Disassociate complaints from community issues, delete and flush
        List<Complaint> existing = complaintRepository.findAll();
        for (Complaint c : existing) {
            c.setCommunityIssue(null);
        }
        complaintRepository.saveAllAndFlush(existing);
        complaintRepository.deleteAllInBatch();
        communityIssueRepository.deleteAllInBatch();
        complaintRepository.flush();

        // 2. Fetch or ensure standard departments (IDs 1-5 from V2 seed)
        Department deptPolice = departmentRepository.findById(1L).orElseGet(() -> departmentRepository.findAll().get(0));
        Department deptCyber = departmentRepository.findById(2L).orElseGet(() -> departmentRepository.findAll().get(1));
        Department deptPwd = departmentRepository.findById(3L).orElseGet(() -> departmentRepository.findAll().get(2));
        Department deptMun = departmentRepository.findById(4L).orElseGet(() -> departmentRepository.findAll().get(3));
        Department deptEle = departmentRepository.findById(5L).orElseGet(() -> departmentRepository.findAll().get(4));

        // 3. Fetch officers
        Officer offPolice = officerRepository.findById(1L).orElse(null);
        Officer offCyber = officerRepository.findById(2L).orElse(null);
        Officer offPwd = officerRepository.findById(3L).orElse(null);
        Officer offMun = officerRepository.findById(4L).orElse(null);
        Officer offEle = officerRepository.findById(5L).orElse(null);

        // 4. Fetch or create citizens
        User defaultCitizen = userRepository.findByUsername("citizen_kavitha")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("citizen_kavitha")
                        .fullName("Kavitha Deshmukh")
                        .phoneNumber("+91 98450 12345")
                        .email("kavitha.citizen@example.com")
                        .passwordHash("$2a$10$PvvzdkTkC/OcOK1IZecgUOD5TrQk4xFDuq3wdcA1y23EGDStbLHOm")
                        .role(Role.CITIZEN)
                        .enabled(true)
                        .build()));

        User citizenRamesh = getOrCreateCitizen("Ramesh Hegde", "+91 94480 23451", "ramesh.hegde@example.com");
        User citizenSneha = getOrCreateCitizen("Sneha Murthy", "+91 94480 34562", "sneha.murthy@example.com");
        User citizenVenkatesh = getOrCreateCitizen("Venkatesh Gowda", "+91 94480 45673", "venkatesh.g@example.com");
        User citizenSunita = getOrCreateCitizen("Sunita Rao", "+91 94480 56784", "sunita.rao@example.com");
        User citizenPradeep = getOrCreateCitizen("Pradeep Kumar", "+91 94480 67895", "pradeep.k@example.com");
        User citizenAnand = getOrCreateCitizen("Anand Kulkarni", "+91 94480 78906", "anand.k@example.com");

        // 5. Create Community Issue for Indiranagar Streetlights (Duplicates Cluster)
        CommunityIssue streetlightCluster = CommunityIssue.builder()
                .title("Dark stretch due to faulty streetlights on Indiranagar 100 Feet Road")
                .category("Streetlight & Power")
                .department(deptEle)
                .locationAddress("100 Feet Road, Indiranagar, Bengaluru")
                .latitude(new BigDecimal("12.971891"))
                .longitude(new BigDecimal("77.641151"))
                .status("OPEN")
                .build();
        streetlightCluster = communityIssueRepository.save(streetlightCluster);

        List<Complaint> seededComplaints = new ArrayList<>();

        // ---------------------------------------------------------------------
        // POLICE PILOT: Cybercrime & Financial Fraud (1-3, 10-11)
        // ---------------------------------------------------------------------
        // 1. Cybercrime (EN) - Assigned
        seededComplaints.add(buildComplaint(
                "RSK-2026-001", citizenSneha, deptCyber, offCyber,
                "Phishing SMS targeting SBI YONO credentials",
                "Received deceptive SMS alleging electricity disconnection unless link is clicked. Clicked link led to fraudulent page seeking MPIN.",
                "Cybercrime & Online Fraud", Priority.HIGH, ComplaintStatus.ASSIGNED,
                new BigDecimal("12.9352"), new BigDecimal("77.6245"), "Koramangala 4th Block, Bengaluru",
                now.plusDays(3), 85, 0, null, null, null
        ));

        // 2. Financial Fraud (KN) - In Progress
        seededComplaints.add(buildComplaint(
                "RSK-2026-002", citizenRamesh, deptCyber, offCyber,
                "ಆನ್‌ಲೈನ್ UPI ಕ್ಯೂಆರ್ ಕೋಡ್ ವಂಚನೆ ₹45,000",
                "ಓಎಲ್ಎಕ್ಸ್‌ನಲ್ಲಿ ಹಳೆಯ ಪೀಠೋಪಕರಣ ಮಾರಾಟ ಮಾಡುವಾಗ ವ್ಯಕ್ತಿಯೊಬ್ಬ ಕ್ಯೂಆರ್ ಕೋಡ್ ಸ್ಕ್ಯಾನ್ ಮಾಡಲು ಹೇಳಿ ಖಾತೆಯಿಂದ ₹45,000 ಹಣ ಕಡಿತಗೊಳಿಸಿದ್ದಾನೆ.",
                "Financial Fraud", Priority.HIGH, ComplaintStatus.IN_PROGRESS,
                new BigDecimal("12.9784"), new BigDecimal("77.5946"), "ಮಲ್ಲೇಶ್ವರಂ 8ನೇ ಕ್ರಾಸ್, ಬೆಂಗಳೂರು",
                now.plusDays(2), 75, 0, null, null, null
        ));

        // 3. Financial Fraud (EN) - New
        seededComplaints.add(buildComplaint(
                "RSK-2026-003", citizenPradeep, deptCyber, null,
                "Fake Telegram Part-time Job Investment Scam",
                "Lured into Telegram crypto rating task with promise of 30% daily returns. Invested ₹1,20,000 which is now locked by fraudsters.",
                "Financial Fraud", Priority.MEDIUM, ComplaintStatus.NEW,
                new BigDecimal("12.9698"), new BigDecimal("77.7500"), "Whitefield Main Road, Bengaluru",
                now.plusDays(5), 100, 0, null, null, null
        ));

        // ---------------------------------------------------------------------
        // POLICE PILOT: Lost Property & Public Safety & Traffic (4-9, 12)
        // ---------------------------------------------------------------------
        // 4. Lost Property (KN) - Resolved
        seededComplaints.add(buildComplaint(
                "RSK-2026-004", citizenVenkatesh, deptPolice, offPolice,
                "ಬಿಎಂಟಿಸಿ ಬಸ್‌ನಲ್ಲಿ ಕಳೆದುಹೋದ ಲ್ಯಾಪ್‌ಟಾಪ್ ಮತ್ತು ಆಧಾರ್ ಕಾರ್ಡ್",
                "ಮೆಜೆಸ್ಟಿಕ್‌ನಿಂದ ಜಯನಗರಕ್ಕೆ ತೆರಳುವ ಬಿಎಂಟಿಸಿ ಮಾರ್ಗ 215 ಬಸ್‌ನಲ್ಲಿ ಕಪ್ಪು ಬಣ್ಣದ ಡೆಲ್ ಲ್ಯಾಪ್‌ಟಾಪ್ ಬ್ಯಾಗ್ ಮತ್ತು ಆಧಾರ್ ದಾಖಲೆಗಳು ತಪ್ಪಿಹೋಗಿವೆ.",
                "Lost Property", Priority.LOW, ComplaintStatus.RESOLVED,
                new BigDecimal("12.9767"), new BigDecimal("77.5713"), "ಕೆಂಪೇಗೌಡ ಬಸ್ ನಿಲ್ದಾಣ, ಮೆಜೆಸ್ಟಿಕ್, ಬೆಂಗಳೂರು",
                now.plusDays(7), 100, 0, "ಬಸ್ ಡಿಪೋ ಕಂಟ್ರೋಲ್ ರೂಂ ಸಂಪರ್ಕಿಸಿ ಲ್ಯಾಪ್‌ಟಾಪ್ ಪತ್ತೆಹಚ್ಚಿ ವಾರಸುದಾರರಿಗೆ ಹಸ್ತಾಂತರಿಸಲಾಗಿದೆ.", now.minusDays(1), null
        ));

        // 5. Lost Property (EN) - In Progress
        seededComplaints.add(buildComplaint(
                "RSK-2026-005", defaultCitizen, deptPolice, offPolice,
                "Lost Leather Wallet with Driving License and Metro Smart Card",
                "Misplaced brown leather wallet while deboarding Namma Metro at MG Road Station. Contains original DL, voter ID, and SBI debit card.",
                "Lost Property", Priority.LOW, ComplaintStatus.IN_PROGRESS,
                new BigDecimal("12.9755"), new BigDecimal("77.6066"), "MG Road Metro Station, Bengaluru",
                now.plusDays(4), 90, 0, null, null, null
        ));

        // 6. Public Safety (EN) - Assigned
        seededComplaints.add(buildComplaint(
                "RSK-2026-006", citizenAnand, deptPolice, offPolice,
                "Reckless Late-Night Drag Racing and Modified Exhaust Disturbance",
                "Modified superbikes and sports cars engaging in illegal speed trials along Kasturba Road between 11 PM and 2 AM, endangering pedestrians.",
                "Public Safety", Priority.MEDIUM, ComplaintStatus.ASSIGNED,
                new BigDecimal("12.9734"), new BigDecimal("77.5912"), "Kasturba Road, Near Cubbon Park, Bengaluru",
                now.plusDays(4), 85, 0, null, null, null
        ));

        // 7. Public Safety (KN) - In Progress
        seededComplaints.add(buildComplaint(
                "RSK-2026-007", citizenSunita, deptPolice, offPolice,
                "ಮಹಿಳೆಯರ ಹಾಸ್ಟೆಲ್ ಬಳಿ ಪುಂಡರ ಉಪಟಳ ಮತ್ತು ಚುಡಾಯಿಸುವಿಕೆ",
                "ಸಂಜೆ ವೇಳೆ ಮಹಿಳಾ ಹಾಸ್ಟೆಲ್ ಬಳಿ ಬೈಕ್‌ಗಳಲ್ಲಿ ನಿಂತು ಅಸಭ್ಯವಾಗಿ ವರ್ತಿಸುವ ಕಿಡಿಗೇಡಿಗಳ ಹಾವಳಿ ಹೆಚ್ಚಾಗಿದೆ. ತಕ್ಷಣ ಬೀಟ್ ಗಸ್ತು ಹೆಚ್ಚಿಸಬೇಕು.",
                "Public Safety", Priority.HIGH, ComplaintStatus.IN_PROGRESS,
                new BigDecimal("12.9141"), new BigDecimal("77.6109"), "ಬಿಟಿಎಂ ಲೇಔಟ್ 2ನೇ ಹಂತ, ಬೆಂಗಳೂರು",
                now.plusDays(2), 70, 0, null, null, null
        ));

        // 8. Traffic (EN) - New
        seededComplaints.add(buildComplaint(
                "RSK-2026-008", citizenSneha, deptPolice, null,
                "Dangerous Wrong-Side Driving and Signal Jumping on Outer Ring Road",
                "Two-wheelers and auto-rickshaws frequently taking wrong side near Bellandur flyover to avoid U-turn, creating fatal blind spot collisions.",
                "Traffic Safety", Priority.MEDIUM, ComplaintStatus.NEW,
                new BigDecimal("12.9260"), new BigDecimal("77.6762"), "Outer Ring Road, Bellandur Junction, Bengaluru",
                now.plusDays(6), 100, 0, null, null, null
        ));

        // 9. Traffic (KN) - Resolved
        seededComplaints.add(buildComplaint(
                "RSK-2026-009", citizenRamesh, deptPolice, offPolice,
                "ಸರಕಾರಿ ಪ್ರಾಥಮಿಕ ಶಾಲೆ ಮುಂಭಾಗ ಝೀಬ್ರಾ ಕ್ರಾಸಿಂಗ್ ಮತ್ತು ವೇಗ ನಿಯಂತ್ರಕ ಅಗತ್ಯವಿದೆ",
                "ಮುಖ್ಯ ರಸ್ತೆಯಲ್ಲಿ ಶಾಲೆ ಇರುವ ಕಾರಣ ಮಕ್ಕಳು ರಸ್ತೆ ದಾಟಲು ತೊಂದರೆಯಾಗಿದೆ. ವೈಜ್ಞಾನಿಕ ಹಂಪ್ ಮತ್ತು ಪಾದಚಾರಿ ರೇಖೆ ಅಳವಡಿಸಲು ಮನವಿ.",
                "Traffic Safety", Priority.LOW, ComplaintStatus.RESOLVED,
                new BigDecimal("12.9915"), new BigDecimal("77.5552"), "ರಾಜಾಜಿನಗರ 4ನೇ ಬ್ಲಾಕ್, ಬೆಂಗಳೂರು",
                now.plusDays(7), 100, 0, "ಸಂಚಾರ ಪೊಲೀಸರ ಅನುಮೋದನೆಯೊಂದಿಗೆ ಬಿಬಿಎಂಪಿ ವತಿಯಿಂದ ರಬ್ಬರ್ ವೇಗ ನಿಯಂತ್ರಕ ಅಳವಡಿಸಲಾಗಿದೆ.", now.minusDays(2), null
        ));

        // 10. Cybercrime (EN) - Resolution Submitted
        seededComplaints.add(buildComplaint(
                "RSK-2026-010", citizenPradeep, deptCyber, offCyber,
                "Unauthorized International Credit Card Transactions from Bogus Merchant",
                "Three transactions totaling $412 charged on HDFC card from unknown Dublin merchant without OTP generation. Card blocked.",
                "Cybercrime & Online Fraud", Priority.HIGH, ComplaintStatus.RESOLUTION_SUBMITTED,
                new BigDecimal("12.9815"), new BigDecimal("77.6408"), "Indiranagar Defense Colony, Bengaluru",
                now.plusDays(3), 85, 0, "Subpoena issued under Section 91 CrPC to merchant payment gateway. Disputed charges reversed by card issuer.", now.minusHours(4), null
        ));

        // 11. Financial Fraud (KN) - In Progress
        seededComplaints.add(buildComplaint(
                "RSK-2026-011", citizenSunita, deptCyber, offCyber,
                "ನಕಲಿ ಲೋನ್ ಆ್ಯಪ್ ಮೂಲಕ ಕಿರುಕುಳ ಮತ್ತು ಬ್ಲ್ಯಾಕ್‌ಮೇಲ್",
                "₹5,000 ಸಾಲ ಪಡೆದ ನಂತರ ಕಾಂಟ್ಯಾಕ್ಟ್ ಲಿಸ್ಟ್ ಹ್ಯಾಕ್ ಮಾಡಿ ಮಾರ್ಫ್ ಮಾಡಿದ ಫೋಟೋಗಳನ್ನು ಸಂಬಂಧಿಕರಿಗೆ ಕಳುಹಿಸಿ ಬೆದರಿಕೆ ಹಾಕುತ್ತಿದ್ದಾರೆ.",
                "Financial Fraud", Priority.HIGH, ComplaintStatus.IN_PROGRESS,
                new BigDecimal("12.9344"), new BigDecimal("77.5344"), "ಬನಶಂಕರಿ 3ನೇ ಹಂತ, ಬೆಂಗಳೂರು",
                now.plusDays(2), 65, 0, null, null, null
        ));

        // 12. Public Safety (EN) - Resolved
        seededComplaints.add(buildComplaint(
                "RSK-2026-012", citizenAnand, deptPolice, offPolice,
                "Unattended suspicious metal briefcase near commercial complex",
                "Silver briefcase left near basement entrance for over 4 hours with no owner in vicinity.",
                "Public Safety", Priority.MEDIUM, ComplaintStatus.RESOLVED,
                new BigDecimal("12.9719"), new BigDecimal("77.6000"), "Residency Road, Central Bengaluru",
                now.plusDays(5), 100, 0, "Anti-sabotage squad inspected briefcase; contained non-hazardous architectural blueprints. Safely retained at station.", now.minusDays(1), null
        ));

        // ---------------------------------------------------------------------
        // CIVIC CATEGORIES: Municipal (BBMP) & Public Works (PWD) (13-15, 18-22)
        // ---------------------------------------------------------------------
        // 13. Municipal (EN) - In Progress
        seededComplaints.add(buildComplaint(
                "RSK-2026-013", defaultCitizen, deptMun, offMun,
                "Deep hazardous pothole on Silk Board Flyover approach ramp",
                "Large 1.5-foot deep pothole with exposed aggregate causing severe bumper damage and sudden braking during peak traffic.",
                "Roads & Potholes", Priority.HIGH, ComplaintStatus.IN_PROGRESS,
                new BigDecimal("12.9172"), new BigDecimal("77.6228"), "Silk Board Junction, Hosur Road, Bengaluru",
                now.plusDays(2), 70, 0, null, null, null
        ));

        // 14. Municipal (KN) - Reopened (Prompt 7.3 Verified)
        seededComplaints.add(buildComplaint(
                "RSK-2026-014", citizenRamesh, deptMun, offMun,
                "ಮಳೆ ನೀರಿನ ಚರಂಡಿ ಕಟ್ಟಿಕೊಂಡು ರಸ್ತೆಗೆ ಹರಿಯುತ್ತಿರುವ ಕೊಳಚೆ ನೀರು",
                "ಚರಂಡಿಯಲ್ಲಿ ಹೂಳು ತುಂಬಿ ಮಳೆ ಬಂದಾಗ ಕೊಳಚೆ ನೀರು ಮನೆಗಳ ಮುಂದೆ ನಿಲ್ಲುತ್ತಿದೆ. ಸೊಳ್ಳೆಗಳ ಕಾಟ ಹೆಚ್ಚಾಗಿದೆ.",
                "Water & Drainage", Priority.HIGH, ComplaintStatus.REOPENED,
                new BigDecimal("13.0827"), new BigDecimal("77.5564"), "ವಿದ್ಯಾರಣ್ಯಪುರ ಮುಖ್ಯ ರಸ್ತೆ, ಬೆಂಗಳೂರು",
                now.plusDays(3), 50, 1, "ಹೂಳೆತ್ತುವ ವಾಹನದಿಂದ ಚರಂಡಿ ಸ್ವಚ್ಛಗೊಳಿಸಲಾಗಿದೆ ಎಂದು ತಿಳಿಸಲಾಗಿತ್ತು, ಆದರೆ ನಾಗರಿಕರು ಅರ್ಧ ಕೆಲಸವಾಗಿದೆ ಎಂದು ಮರುಪರಿಶೀಲನೆ ಕೋರಿದ್ದಾರೆ.", now.minusDays(1), null
        ));

        // 15. Public Works (EN) - Assigned
        seededComplaints.add(buildComplaint(
                "RSK-2026-015", citizenSneha, deptPwd, offPwd,
                "Structural concrete spalling and exposed rebar on foot-over bridge",
                "Underneath the pedestrian bridge deck, large chunks of plaster have fallen onto the carriageway below. Rusting steel rebar visible.",
                "Public Works & Infrastructure", Priority.HIGH, ComplaintStatus.ASSIGNED,
                new BigDecimal("13.0068"), new BigDecimal("77.5813"), "Mekhri Circle Foot-Over Bridge, Bengaluru",
                now.plusDays(4), 80, 0, null, null, null
        ));

        // 16. Electricity (EN) - CRITICAL COMPLAINT 1 (Health Score 20 - Critical Band)
        Complaint criticalComplaint1 = buildComplaint(
                "RSK-2026-016", citizenVenkatesh, deptEle, offEle,
                "Sparking overhead 11kV distribution line hanging low near residential balcony",
                "High-tension 11kV conductor snapped from insulator pin and is sagging within 1 meter of 2nd-floor residential balcony. Visible sparking on contact with tree branch.",
                "Streetlight & Power", Priority.CRITICAL, ComplaintStatus.IN_PROGRESS,
                new BigDecimal("12.9304"), new BigDecimal("77.5838"), "9th Main, Jayanagar 4th Block, Bengaluru",
                now.plusHours(4), 20, 0, null, null, null
        );
        seededComplaints.add(criticalComplaint1);

        // 17. Electricity (KN) - CRITICAL COMPLAINT 2 (Health Score 25 - Critical Band)
        Complaint criticalComplaint2 = buildComplaint(
                "RSK-2026-017", citizenAnand, deptEle, offEle,
                "ಟ್ರಾನ್ಸ್‌ಫಾರ್ಮರ್ ಸ್ಫೋಟಗೊಂಡು ಸರಕಾರಿ ತಾಲೂಕು ಆಸ್ಪತ್ರೆ ಫೀಡರ್ ವಿದ್ಯುತ್ ಸ್ಥಗಿತ",
                "ಬೆಳಗ್ಗೆ 100kVA ಟ್ರಾನ್ಸ್‌ಫಾರ್ಮರ್ ದೊಡ್ಡ ಶಬ್ದದೊಂದಿಗೆ ಸ್ಫೋಟಗೊಂಡಿದೆ. ತಾಲೂಕು ಆಸ್ಪತ್ರೆಯ ಐಸಿಯು ಮತ್ತು ಆಕ್ಸಿಜನ್ ಯುನಿಟ್‌ಗೆ ವಿದ್ಯುತ್ ಸರಬರಾಜು ಸ್ಥಗಿತಗೊಂಡಿದೆ.",
                "Streetlight & Power", Priority.CRITICAL, ComplaintStatus.ASSIGNED,
                new BigDecimal("13.0358"), new BigDecimal("77.5970"), "ಹೆಬ್ಬಾಳ ಮುಖ್ಯ ರಸ್ತೆ, ಆಸ್ಪತ್ರೆ ಎದುರು, ಬೆಂಗಳೂರು",
                now.plusHours(2), 25, 0, null, null, null
        );
        seededComplaints.add(criticalComplaint2);

        // 18. Public Works (EN) - ALREADY ESCALATED COMPLAINT (Level 1 Escalation Record)
        Complaint escalatedComplaint = buildComplaint(
                "RSK-2026-018", citizenPradeep, deptPwd, offPwd,
                "Stormwater drain retention wall collapse hazard threatening adjacent arterial road",
                "A 20-meter stretch of the primary stormwater drain stone masonry wall has bowed outward following heavy runoff. Soil subsidence observed beneath the pedestrian footpath.",
                "Public Works & Infrastructure", Priority.HIGH, ComplaintStatus.OVERDUE,
                new BigDecimal("12.9880"), new BigDecimal("77.5350"), "Magadi Main Road, Tollgate, Bengaluru",
                now.minusDays(3), 30, 0, null, null, null
        );
        escalatedComplaint.setEscalationLevel(1);
        seededComplaints.add(escalatedComplaint);

        // 19. Municipal (KN) - In Progress
        seededComplaints.add(buildComplaint(
                "RSK-2026-019", citizenSunita, deptMun, offMun,
                "ಕಲುಷಿತ ಕುಡಿಯುವ ನೀರು ಸರಬರಾಜು - ಕೊಳವೆಬಾವಿ ನೀರಿನಲ್ಲಿ ದುರ್ವಾಸನೆ",
                "ಕಳೆದ ನಾಲ್ಕು ದಿನಗಳಿಂದ ನಲ್ಲಿಯಲ್ಲಿ ಹಳದಿ ಬಣ್ಣದ ಕಲುಷಿತ ನೀರು ಬರುತ್ತಿದೆ. ಒಳಚರಂಡಿ ಪೈಪ್‌ಲೈನ್ ಮಿಶ್ರಣವಾಗಿರುವ ಶಂಕೆ ಇದೆ.",
                "Water & Drainage", Priority.HIGH, ComplaintStatus.IN_PROGRESS,
                new BigDecimal("12.9610"), new BigDecimal("77.5385"), "ವಿಜಯನಗರ 2ನೇ ಹಂತ, ಬೆಂಗಳೂರು",
                now.plusDays(3), 60, 0, null, null, null
        ));

        // 20. Public Works (KN) - New
        seededComplaints.add(buildComplaint(
                "RSK-2026-020", citizenRamesh, deptPwd, null,
                "ರಾಷ್ಟ್ರೀಯ ಹೆದ್ದಾರಿ ಸಂಪರ್ಕ ರಸ್ತೆ ಡಾಂಬರೀಕರಣ ಅರ್ಧಕ್ಕೆ ನಿಂತು ದೂಳು ಮತ್ತು ಅಪಘಾತ",
                "ರಸ್ತೆ ಅಗಲೀಕರಣ ಕಾಮಗಾರಿ ಅರ್ಧಕ್ಕೆ ನಿಂತಿದ್ದು, ಜಲ್ಲಿಕಲ್ಲುಗಳು ರಸ್ತೆ ತುಂಬ ಹರಡಿಕೊಂಡಿವೆ. ದ್ವಿಚಕ್ರ ವಾಹನಗಳು ಸ್ಕಿಡ್ ಆಗಿ ಬೀಳುತ್ತಿವೆ.",
                "Public Works & Infrastructure", Priority.MEDIUM, ComplaintStatus.NEW,
                new BigDecimal("13.0450"), new BigDecimal("77.5650"), "ಯಲಹಂಕ ನ್ಯೂ ಟೌನ್, ಬೆಂಗಳೂರು",
                now.plusDays(6), 100, 0, null, null, null
        ));

        // 21. Municipal (EN) - Resolution Submitted
        seededComplaints.add(buildComplaint(
                "RSK-2026-021", defaultCitizen, deptMun, offMun,
                "Overflowing commercial garbage heap and bio-waste dumped in vacant lot",
                "Hotel waste dumped on open corner site attracting stray cattle and pack of dogs, blocking pedestrian access.",
                "Solid Waste Management", Priority.MEDIUM, ComplaintStatus.RESOLUTION_SUBMITTED,
                new BigDecimal("12.9550"), new BigDecimal("77.6000"), "Richmond Town, Langford Road, Bengaluru",
                now.plusDays(4), 85, 0, "BBMP compactors cleared 4 tonnes of waste; site barricaded with statutory non-dumping penalty warning signboards.", now.minusHours(6), null
        ));

        // 22. Public Works (EN) - Assigned
        seededComplaints.add(buildComplaint(
                "RSK-2026-022", citizenSneha, deptPwd, offPwd,
                "Pedestrian walkway tactile pavers broken during underground cable laying",
                "Telecom contractor excavated interlock paving tiles along 100m corridor and abandoned without backfilling or compaction.",
                "Public Works & Infrastructure", Priority.LOW, ComplaintStatus.ASSIGNED,
                new BigDecimal("12.9700"), new BigDecimal("77.6100"), "Brigade Road, Commercial Junction, Bengaluru",
                now.plusDays(5), 90, 0, null, null, null
        ));

        // ---------------------------------------------------------------------
        // THREE POTENTIAL DUPLICATES FOR STREETLIGHT ISSUE IN SAME AREA (23-25)
        // Indiranagar 100 Feet Road corridor within 30 meters of each other
        // ---------------------------------------------------------------------
        // 23. Duplicate 1 (EN) - In Progress (Linked to Community Issue)
        Complaint duplicate1 = buildComplaint(
                "RSK-2026-023", citizenPradeep, deptEle, offEle,
                "Non-functioning streetlight pole #42 on Indiranagar 100ft Road opposite KFC",
                "Streetlight fixture on pole #42 has been completely dark for 5 days. Heavy vehicular and pedestrian crossing at night.",
                "Streetlight & Power", Priority.MEDIUM, ComplaintStatus.IN_PROGRESS,
                new BigDecimal("12.971891"), new BigDecimal("77.641151"), "100 Feet Rd, Opposite KFC, Indiranagar, Bengaluru",
                now.plusDays(2), 75, 0, null, null, streetlightCluster
        );
        seededComplaints.add(duplicate1);

        // 24. Duplicate 2 (KN) - New (Linked to Community Issue)
        Complaint duplicate2 = buildComplaint(
                "RSK-2026-024", citizenRamesh, deptEle, null,
                "ಇಂದಿರಾನಗರ 100 ಅಡಿ ರಸ್ತೆಯಲ್ಲಿ ಬೀದಿದೀಪ ಕೆಟ್ಟುಹೋಗಿ ಕತ್ತಲೆ ಉಂಟಾಗಿದೆ streetlight dark",
                "ಕೆಎಫ್‌ಸಿ ಜಂಕ್ಷನ್ ಬಳಿ ಬೀದಿದೀಪಗಳು ಉರಿಯುತ್ತಿಲ್ಲ. ರಾತ್ರಿ ವೇಳೆ ಮಹಿಳೆಯರು ಮತ್ತು ಪಾದಚಾರಿಗಳು ಓಡಾಡಲು ಭಯವಾಗುತ್ತದೆ.",
                "Streetlight & Power", Priority.MEDIUM, ComplaintStatus.NEW,
                new BigDecimal("12.971950"), new BigDecimal("77.641200"), "100 Feet Road, Near KFC, Indiranagar, Bengaluru",
                now.plusDays(3), 100, 0, null, null, streetlightCluster
        );
        seededComplaints.add(duplicate2);

        // 25. Duplicate 3 (EN) - New (Linked to Community Issue)
        Complaint duplicate3 = buildComplaint(
                "RSK-2026-025", defaultCitizen, deptEle, null,
                "Streetlight completely off on 100 Feet Road Indiranagar near KFC junction pitch dark at night",
                "Continuous pitch black stretch on 100ft road near KFC crossing due to burned out LED luminaire. High accident hazard.",
                "Streetlight & Power", Priority.MEDIUM, ComplaintStatus.NEW,
                new BigDecimal("12.971820"), new BigDecimal("77.641100"), "100 Feet Rd, Indiranagar, Bengaluru",
                now.plusDays(3), 100, 0, null, null, streetlightCluster
        );
        seededComplaints.add(duplicate3);

        // 6. Save all 25 complaints
        List<Complaint> savedComplaints = complaintRepository.saveAll(seededComplaints);

        // 7. Write Action History and specialized records
        for (Complaint c : savedComplaints) {
            // Baseline registration history
            actionHistoryRepository.save(new ActionHistory(
                    null, c.getId(), c.getCitizen().getId(), c.getCitizen().getFullName(),
                    "CITIZEN", "REGISTERED", null, "NEW",
                    "Grievance lodged via portal under SAKALA Services Act. Tracking ID: " + c.getTrackingId(),
                    false, now.minusDays(5)
            ));

            // Assignment history if assigned
            if (c.getAssignedOfficer() != null) {
                actionHistoryRepository.save(new ActionHistory(
                        null, c.getId(), 1L, "System Administrator",
                        "ADMIN", "ASSIGNED", "NEW", c.getStatus().name(),
                        "Assigned to " + getOfficerName(c.getAssignedOfficer()) + " for field inspection and statutory redressal.",
                        false, now.minusDays(4)
                ));
            }

            // Progress note history if in-progress or resolved
            if (c.getStatus() == ComplaintStatus.IN_PROGRESS && c.getAssignedOfficer() != null) {
                actionHistoryRepository.save(new ActionHistory(
                        null, c.getId(), getOfficerUserId(c.getAssignedOfficer()), getOfficerName(c.getAssignedOfficer()),
                        "OFFICER", "PROGRESS_UPDATE", "ASSIGNED", "IN_PROGRESS",
                        "Field team mobilized to location. Initial technical assessment completed.",
                        false, now.minusDays(2)
                ));
            } else if (c.getStatus() == ComplaintStatus.RESOLUTION_SUBMITTED && c.getAssignedOfficer() != null) {
                actionHistoryRepository.save(new ActionHistory(
                        null, c.getId(), getOfficerUserId(c.getAssignedOfficer()), getOfficerName(c.getAssignedOfficer()),
                        "OFFICER", "RESOLUTION_SUBMITTED", "IN_PROGRESS", "RESOLUTION_SUBMITTED",
                        c.getResolutionSummary(), false, now.minusHours(4)
                ));
            } else if (c.getStatus() == ComplaintStatus.RESOLVED) {
                actionHistoryRepository.save(new ActionHistory(
                        null, c.getId(), c.getCitizen().getId(), c.getCitizen().getFullName(),
                        "CITIZEN", "CONFIRMED_RESOLUTION", "RESOLUTION_SUBMITTED", "RESOLVED",
                        "Citizen inspected on-site resolution and confirmed satisfactory redressal.", false, now.minusDays(1)
                ));
            } else if (c.getStatus() == ComplaintStatus.REOPENED) {
                actionHistoryRepository.save(new ActionHistory(
                        null, c.getId(), c.getCitizen().getId(), c.getCitizen().getFullName(),
                        "CITIZEN", "REJECTED_RESOLUTION", "RESOLUTION_SUBMITTED", "REOPENED",
                        "Citizen disputed resolution proof. Docket reopened under statutory re-inspection mandate.",
                        false, now.minusHours(12)
                ));
            }
        }

        // 8. Create Escalation record for Complaint 18 (Escalated Complaint)
        Complaint complaint18 = savedComplaints.stream()
                .filter(c -> "RSK-2026-018".equals(c.getTrackingId()))
                .findFirst()
                .orElse(null);

        if (complaint18 != null) {
            Escalation escalation = Escalation.builder()
                    .complaint(complaint18)
                    .fromLevel(0)
                    .toLevel(1)
                    .triggerReason("Statutory SLA breached past 48h grace period without officer progress update")
                    .notifiedSupervisorId(3L) // Executive Engineer Shivakumar M (PWD)
                    .acknowledged(false)
                    .build();
            escalationRepository.save(escalation);

            actionHistoryRepository.save(new ActionHistory(
                    null, complaint18.getId(), null, "SYSTEM",
                    "SYSTEM", "ESCALATED_LEVEL_1", "IN_PROGRESS", "OVERDUE",
                    "Automated Tier-1 escalation to Departmental Supervisor under Karnataka SAKALA Services Act § 7.",
                    false, now.minusDays(1)
            ));
        }

        // 9. Reset Tracking Sequences table to 25 so next created complaint is RSK-2026-026
        int currentYear = now.getYear();
        TrackingSequence seq = trackingSequenceRepository.findByYearForUpdate(currentYear)
                .orElseGet(() -> {
                    TrackingSequence s = new TrackingSequence();
                    s.setYear(currentYear);
                    return s;
                });
        seq.setCurrentSequence(25L);
        trackingSequenceRepository.saveAndFlush(seq);

        log.info("Successfully seeded 25 complaints. Next tracking sequence set to 26.");

        return Map.of(
                "success", true,
                "totalComplaintsSeeded", 25,
                "duplicatesLinked", 3,
                "criticalComplaints", 2,
                "escalatedComplaints", 1,
                "reopenedComplaints", 1,
                "nextTrackingId", String.format("RSK-%d-%03d", currentYear, 26)
        );
    }

    private User getOrCreateCitizen(String fullName, String phone, String email) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            String username = "citizen_" + Math.abs(email.hashCode() % 100000);
            return userRepository.save(User.builder()
                    .username(username)
                    .fullName(fullName)
                    .phoneNumber(phone)
                    .email(email)
                    .passwordHash("$2a$10$PvvzdkTkC/OcOK1IZecgUOD5TrQk4xFDuq3wdcA1y23EGDStbLHOm")
                    .role(Role.CITIZEN)
                    .enabled(true)
                    .build());
        });
    }

    private Complaint buildComplaint(
            String trackingId, User citizen, Department dept, Officer officer,
            String title, String description, String category, Priority priority, ComplaintStatus status,
            BigDecimal lat, BigDecimal lon, String address, LocalDateTime deadline,
            int healthScore, int reopenCount, String resolutionSummary, LocalDateTime resolutionSubmittedAt,
            CommunityIssue communityIssue) {

        return Complaint.builder()
                .trackingId(trackingId)
                .trackingNumber(trackingId)
                .citizen(citizen)
                .department(dept)
                .assignedOfficer(officer)
                .title(title)
                .description(description)
                .category(category)
                .priority(priority)
                .status(status)
                .source(ComplaintSource.WEB)
                .latitude(lat)
                .longitude(lon)
                .locationAddress(address)
                .followUpDeadline(deadline)
                .slaDueDate(deadline)
                .escalationLevel(0)
                .aiTriageConfirmed(officer != null)
                .healthScore(healthScore)
                .reopenCount(reopenCount)
                .resolutionSummary(resolutionSummary)
                .resolutionSubmittedAt(resolutionSubmittedAt)
                .communityIssue(communityIssue)
                .build();
    }

    private String getOfficerName(Officer officer) {
        if (officer == null) return "Field Officer";
        if (officer.getId() != null) {
            switch (officer.getId().intValue()) {
                case 1: return "Inspector Rajesh Kumar";
                case 2: return "Sub-Inspector Ananya Rao";
                case 3: return "Asst. Executive Engineer Suresh Gowda";
                case 4: return "Health Inspector Manjunath K";
                case 5: return "Section Officer Ramesh B";
            }
        }
        return officer.getDesignation() != null ? officer.getDesignation() : "Field Officer";
    }

    private Long getOfficerUserId(Officer officer) {
        if (officer == null) return 1L;
        if (officer.getId() != null) {
            switch (officer.getId().intValue()) {
                case 1: return 4L;
                case 2: return 5L;
                case 3: return 6L;
                case 4: return 7L;
                case 5: return 8L;
            }
        }
        return 1L;
    }
}
