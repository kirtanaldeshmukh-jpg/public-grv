package com.resolvetrack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class P0FlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("P0 End-to-End Flow: Register complaint -> Track -> Login Admin -> Assign -> Login Officer -> Update -> Trigger Cycle")
    void testCompleteP0Flow() throws Exception {
        // Step 1: Citizen files complaint (Public endpoint)
        Map<String, Object> complaintPayload = Map.of(
                "title", "Main Water Pipe Rupture on 8th Cross Road",
                "description", "Major drinking water leakage causing street flooding and low pressure in homes.",
                "category", "water_leakage",
                "priority", "HIGH",
                "citizenName", "Ananya Sharma",
                "citizenPhone", "9845089201",
                "citizenEmail", "ananya@example.com",
                "locationAddress", "Doddanekkundi, Mahadevapura, Bengaluru Urban"
        );

        MvcResult createResult = mockMvc.perform(post("/api/public/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(complaintPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        assertTrue(createNode.get("success").asBoolean());
        String trackingId = createNode.get("data").get("trackingNumber").asText();
        assertNotNull(trackingId);
        assertTrue(trackingId.startsWith("RSK-"), "Tracking ID must follow RSK format: " + trackingId);

        // Step 2: Citizen tracks grievance via Public Tracking endpoint
        MvcResult trackResult = mockMvc.perform(get("/api/public/track/" + trackingId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode trackNode = objectMapper.readTree(trackResult.getResponse().getContentAsString());
        assertEquals("NEW", trackNode.get("data").get("status").asText());
        // Verify citizen privacy shield: officer details and internal notes are omitted
        assertFalse(trackNode.get("data").has("assignedOfficerName"));
        assertFalse(trackNode.get("data").has("internalNotes"));

        // Step 3: Admin logs in
        Map<String, String> adminLogin = Map.of("username", "admin", "password", "pass");
        MvcResult adminAuthResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode adminAuthNode = objectMapper.readTree(adminAuthResult.getResponse().getContentAsString());
        String adminToken = adminAuthNode.get("data").get("token").asText();
        assertNotNull(adminToken);

        // Step 4: Admin assigns complaint to Officer Rajesh
        MvcResult searchResult = mockMvc.perform(get("/api/complaints")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", trackingId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode searchNode = objectMapper.readTree(searchResult.getResponse().getContentAsString());
        JsonNode items = searchNode.get("data").get("content");
        assertTrue(items.size() > 0, "Expected at least 1 complaint matching " + trackingId);
        long complaintId = items.get(0).get("id").asLong();

        // Assign to Officer ID 1 (Officer Rajesh)
        Map<String, Object> assignPayload = Map.of(
                "officerId", 1L,
                "reassignReason", "Assigned for immediate field inspection and repair by Junior Engineer"
        );
        mockMvc.perform(put("/api/complaints/" + complaintId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignPayload)))
                .andExpect(status().isOk());

        // Step 5: Officer logs in
        Map<String, String> officerLogin = Map.of("username", "officer_rajesh", "password", "pass");
        MvcResult officerAuthResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(officerLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String officerToken = objectMapper.readTree(officerAuthResult.getResponse().getContentAsString()).get("data").get("token").asText();
        assertNotNull(officerToken);

        // Step 6: Officer updates complaint progress
        Map<String, Object> updatePayload = Map.of(
                "status", "IN_PROGRESS",
                "progressNote", "Maintenance crew mobilized with emergency suction tanker and replacement pipe sleeves.",
                "isPublic", true,
                "nextFollowUpDate", "2026-11-12T10:00:00"
        );
        MvcResult updateResult = mockMvc.perform(post("/api/complaints/" + complaintId + "/updates")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode updateNode = objectMapper.readTree(updateResult.getResponse().getContentAsString());
        assertEquals("IN_PROGRESS", updateNode.get("data").get("status").asText());

        // Step 7: Trigger Follow-up cycle manually
        MvcResult cycleResult = mockMvc.perform(post("/api/dev/demo/run-cycle"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cycleNode = objectMapper.readTree(cycleResult.getResponse().getContentAsString());
        assertTrue(cycleNode.get("success").asBoolean());

        // Step 8: Verify Dashboard summary reflects the updated in-progress count
        MvcResult summaryResult = mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode summaryNode = objectMapper.readTree(summaryResult.getResponse().getContentAsString());
        assertTrue(summaryNode.get("data").get("inProgress").asInt() >= 1);
    }
}
