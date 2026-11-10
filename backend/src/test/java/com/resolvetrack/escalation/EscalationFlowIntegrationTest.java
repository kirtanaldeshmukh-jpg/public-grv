package com.resolvetrack.escalation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolvetrack.complaint.Complaint;
import com.resolvetrack.complaint.ComplaintRepository;
import com.resolvetrack.complaint.ComplaintStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EscalationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private EscalationRepository escalationRepository;

    @Test
    @DisplayName("Escalation Integration Flow: Register -> Assign -> Expire Deadline -> Trigger Cycle -> Escalation Tier 1 -> Supervisor View -> Officer Resolution Clears")
    void testCompleteEscalationFlow() throws Exception {
        // Step 1: Citizen files complaint
        Map<String, Object> complaintPayload = Map.of(
                "title", "Severe Road Collapse near Rail Underpass",
                "description", "Stone retention wall collapsed after heavy rain, blocking half the carriageway.",
                "category", "road_collapse",
                "priority", "HIGH",
                "citizenName", "Kiran Hegde",
                "citizenPhone", "9448099881",
                "citizenEmail", "kiran.hegde@example.com",
                "locationAddress", "Yeshwantpur Rail Underpass, Bengaluru"
        );

        MvcResult createResult = mockMvc.perform(post("/api/public/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(complaintPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String trackingId = createNode.get("data").get("trackingNumber").asText();
        assertNotNull(trackingId);

        // Step 2: Admin login & assignment to Officer Suresh (PWD - Officer ID 3)
        MvcResult adminAuthResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "pass"))))
                .andExpect(status().isOk())
                .andReturn();

        String adminToken = objectMapper.readTree(adminAuthResult.getResponse().getContentAsString()).get("data").get("token").asText();

        // Search complaint
        MvcResult searchResult = mockMvc.perform(get("/api/complaints")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", trackingId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper.readTree(searchResult.getResponse().getContentAsString()).get("data").get("content");
        assertTrue(items.size() > 0);
        long complaintId = items.get(0).get("id").asLong();

        // Assign to Officer 3 (PWD)
        mockMvc.perform(put("/api/complaints/" + complaintId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "officerId", 3L,
                                "reassignReason", "Urgent structural assessment assigned to PWD Assistant Executive Engineer"
                        ))))
                .andExpect(status().isOk());

        // Step 3: Fast-forward the complaint deadline into the past to simulate overdue breach
        Complaint complaint = complaintRepository.findById(complaintId).orElseThrow();
        LocalDateTime pastDeadline = LocalDateTime.now().minusHours(72); // 72h past deadline
        complaint.setFollowUpDeadline(pastDeadline);
        complaint.setSlaDueDate(pastDeadline);
        complaintRepository.saveAndFlush(complaint);

        // Step 4: Run Follow-up Cycle
        MvcResult cycleResult = mockMvc.perform(post("/api/dev/demo/run-cycle"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cycleNode = objectMapper.readTree(cycleResult.getResponse().getContentAsString());
        assertTrue(cycleNode.get("success").asBoolean());

        // Step 5: Verify complaint transitioned to OVERDUE or ESCALATED
        Complaint refreshed = complaintRepository.findById(complaintId).orElseThrow();
        assertTrue(refreshed.getStatus() == ComplaintStatus.OVERDUE || refreshed.getStatus() == ComplaintStatus.ESCALATED || refreshed.getEscalationLevel() > 0,
                "Complaint must be flagged overdue or escalated. Current status: " + refreshed.getStatus() + ", level: " + refreshed.getEscalationLevel());

        // Step 6: Supervisor logs in and checks Escalation Center
        MvcResult supAuthResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "supervisor_works", "password", "pass"))))
                .andExpect(status().isOk())
                .andReturn();

        String supToken = objectMapper.readTree(supAuthResult.getResponse().getContentAsString()).get("data").get("token").asText();

        MvcResult escResult = mockMvc.perform(get("/api/escalations")
                        .header("Authorization", "Bearer " + supToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode escNode = objectMapper.readTree(escResult.getResponse().getContentAsString());
        assertTrue(escNode.get("data").isArray());

        // Step 7: Officer logs in and logs progress update
        MvcResult offAuthResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "officer_pwd", "password", "pass"))))
                .andExpect(status().isOk())
                .andReturn();

        String offToken = objectMapper.readTree(offAuthResult.getResponse().getContentAsString()).get("data").get("token").asText();

        Map<String, Object> updatePayload = Map.of(
                "status", "IN_PROGRESS",
                "progressNote", "Emergency shoring and temporary retaining gabion baskets installed. Carriageway stabilized.",
                "isPublic", true,
                "nextFollowUpDate", LocalDateTime.now().plusDays(5).toString()
        );

        mockMvc.perform(post("/api/complaints/" + complaintId + "/updates")
                        .header("Authorization", "Bearer " + offToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk());

        // Step 8: Verify complaint is back to IN_PROGRESS and follow-up deadline is reset
        Complaint afterUpdate = complaintRepository.findById(complaintId).orElseThrow();
        assertEquals(ComplaintStatus.IN_PROGRESS, afterUpdate.getStatus());
        assertTrue(afterUpdate.getFollowUpDeadline().isAfter(LocalDateTime.now()));
    }
}

