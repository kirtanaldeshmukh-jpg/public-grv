package com.resolvetrack.department;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Primary
public class AiSuggestionService implements DepartmentSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(AiSuggestionService.class);

    private static final Set<String> VALID_DEPTS = Set.of("POL", "CYB", "PWD", "MUN", "ELE");
    private static final Set<String> VALID_PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");
    private static final Pattern JSON_EXTRACT_PATTERN = Pattern.compile("\\{.*\\}", Pattern.DOTALL);

    private final RuleBasedDepartmentSuggestionService ruleBasedService;
    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper;
    private HttpClient httpClient;

    @Value("${resolvetrack.ai.api-key:}")
    private String configuredApiKey;

    @Value("${resolvetrack.ai.api-url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String configuredApiUrl;

    public AiSuggestionService(
            RuleBasedDepartmentSuggestionService ruleBasedService,
            DepartmentRepository departmentRepository,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.ruleBasedService = ruleBasedService;
        this.departmentRepository = departmentRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = httpClient;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AiSuggestionService(
            RuleBasedDepartmentSuggestionService ruleBasedService,
            DepartmentRepository departmentRepository,
            ObjectMapper objectMapper) {
        this(ruleBasedService, departmentRepository, objectMapper, null);
    }

    public AiSuggestionService(
            RuleBasedDepartmentSuggestionService ruleBasedService,
            DepartmentRepository departmentRepository) {
        this(ruleBasedService, departmentRepository, new ObjectMapper(), null);
    }

    private synchronized HttpClient getHttpClient() {
        if (this.httpClient == null) {
            try {
                this.httpClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build();
            } catch (Exception e) {
                log.warn("Could not initialize HttpClient: {}", e.getMessage());
            }
        }
        return this.httpClient;
    }

    @Override
    public SuggestionResult suggestDepartment(String category, String text) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("No LLM API key configured in environment. Using rule-based suggestion.");
            return ruleBasedService.suggestDepartment(category, text);
        }

        try {
            // Apply strict 5-second timeout as required by Prompt 8.2
            CompletableFuture<SuggestionResult> future = CompletableFuture.supplyAsync(() -> callLlmApi(category, text, apiKey));
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("AI suggestion call failed or timed out ({}). Seamlessly falling back to rule-based engine.", e.getMessage());
            return ruleBasedService.suggestDepartment(category, text);
        }
    }

    private SuggestionResult callLlmApi(String category, String text, String apiKey) {
        try {
            String prompt = buildPrompt(category, text);
            String requestBody = buildRequestBody(prompt);

            String requestUrl = configuredApiUrl.contains("?")
                    ? configuredApiUrl + "&key=" + apiKey
                    : configuredApiUrl + "?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(4)) // 4s network limit so future timeout handles cleanly
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient client = getHttpClient();
            if (client == null) {
                log.warn("HttpClient unavailable, falling back to rule-based suggestion");
                return ruleBasedService.suggestDepartment(category, text);
            }

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("LLM API returned non-200 HTTP status: {}. Body: {}", response.statusCode(), response.body());
                return ruleBasedService.suggestDepartment(category, text);
            }

            String responseText = extractTextFromLlmResponse(response.body());
            if (responseText == null || responseText.isBlank()) {
                log.warn("LLM response contained no text content");
                return ruleBasedService.suggestDepartment(category, text);
            }

            return parseAndValidateJson(responseText, category, text);
        } catch (Exception e) {
            log.warn("Exception during LLM API invocation: {}", e.getMessage());
            return ruleBasedService.suggestDepartment(category, text);
        }
    }

    public SuggestionResult parseAndValidateJson(String responseText, String fallbackCategory, String fallbackText) {
        try {
            String jsonStr = responseText.trim();
            // Clean markdown fences
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            } else if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();

            // Extract curly braces
            Matcher m = JSON_EXTRACT_PATTERN.matcher(jsonStr);
            if (m.find()) {
                jsonStr = m.group();
            }

            JsonNode root = objectMapper.readTree(jsonStr);

            // 1. Department Validation
            String deptCode = root.path("department").asText("").toUpperCase(Locale.ROOT).trim();
            if (!VALID_DEPTS.contains(deptCode)) {
                // If department returned full name instead of code, try matching
                deptCode = mapDepartmentNameToCode(deptCode);
            }

            if (!VALID_DEPTS.contains(deptCode)) {
                log.warn("LLM suggested invalid department code: {}. Falling back to rule-based.", deptCode);
                return ruleBasedService.suggestDepartment(fallbackCategory, fallbackText);
            }

            // 2. Confidence Validation
            double confidence = root.path("confidence").asDouble(0.85);
            if (confidence <= 0.0 || confidence > 1.0) {
                confidence = 0.85;
            }

            // 3. Priority Validation
            String priority = root.path("priority").asText("MEDIUM").toUpperCase(Locale.ROOT).trim();
            if (!VALID_PRIORITIES.contains(priority)) {
                priority = "MEDIUM";
            }

            // 4. Category, Issue Summary & Location
            String category = root.path("category").asText(fallbackCategory != null ? fallbackCategory : "Civic Grievance");
            String issueSummary = root.path("issueSummary").asText("");
            String extractedLocation = root.path("extractedLocation").asText("");

            Optional<Department> deptOpt = departmentRepository.findByCode(deptCode);
            Long deptId = deptOpt.map(Department::getId).orElse(1L);
            String nameEn = deptOpt.map(Department::getNameEn).orElse("Police Department");
            String nameKn = deptOpt.map(Department::getNameKn).orElse("ಪೊಲೀಸ್ ಇಲಾಖೆ");

            log.info("AI LLM successfully suggested triage: dept={}, confidence={}, priority={}, location={}",
                    deptCode, confidence, priority, extractedLocation);

            return SuggestionResult.builder()
                    .departmentId(deptId)
                    .departmentCode(deptCode)
                    .departmentNameEn(nameEn)
                    .departmentNameKn(nameKn)
                    .confidence(confidence)
                    .priority(priority)
                    .category(category)
                    .issueSummary(issueSummary)
                    .extractedLocation(extractedLocation)
                    .source("AI")
                    .explanation(String.format("AI-assisted classification (confidence: %.0f%%, priority: %s)", confidence * 100, priority))
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse and validate AI JSON suggestion: {}. Falling back to rule-based.", e.getMessage());
            return ruleBasedService.suggestDepartment(fallbackCategory, fallbackText);
        }
    }

    private String buildPrompt(String category, String text) {
        return "You are an automated civic triage classifier for the Government of Karnataka's ResolveTrack portal.\n" +
                "Analyze the following citizen grievance:\n" +
                "Category: " + (category != null ? category : "Unknown") + "\n" +
                "Grievance Text: " + (text != null ? text : "") + "\n\n" +
                "Return ONLY a raw JSON object with NO markdown formatting, NO backticks, and NO surrounding text.\n" +
                "JSON format required:\n" +
                "{\n" +
                "  \"category\": \"normalized category name\",\n" +
                "  \"department\": \"one of [POL, CYB, PWD, MUN, ELE]\",\n" +
                "  \"priority\": \"one of [LOW, MEDIUM, HIGH, URGENT]\",\n" +
                "  \"issueSummary\": \"concise 1-sentence issue summary\",\n" +
                "  \"extractedLocation\": \"extracted street, landmark or ward in Karnataka, or empty string\",\n" +
                "  \"confidence\": 0.95\n" +
                "}\n" +
                "Department Guide:\n" +
                "- POL: Law & order, theft, assault, patrol, missing items\n" +
                "- CYB: Online scams, OTP theft, UPI fraud, bank cybercrime\n" +
                "- PWD: Roads, potholes, highways, bridge maintenance, flyovers\n" +
                "- MUN: Water supply, pipe leaks, sewage, garbage, drainage, BBMP civic matters\n" +
                "- ELE: Power outage, transformer sparks, streetlights, BESCOM billing";
    }

    private String buildRequestBody(String prompt) {
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> payload = Map.of(
                "contents", List.of(content),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "maxOutputTokens", 500
                )
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]}]}";
        }
    }

    private String extractTextFromLlmResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText("");
                }
            }
            // Support OpenAI response format if configured
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                return choices.get(0).path("message").path("content").asText("");
            }
        } catch (Exception e) {
            log.warn("Error extracting text from LLM response body: {}", e.getMessage());
        }
        return null;
    }

    private String mapDepartmentNameToCode(String deptName) {
        if (deptName == null) return "POL";
        String lower = deptName.toLowerCase(Locale.ROOT);
        if (lower.contains("cyber")) return "CYB";
        if (lower.contains("police")) return "POL";
        if (lower.contains("public work") || lower.contains("pwd") || lower.contains("road")) return "PWD";
        if (lower.contains("municipal") || lower.contains("bbmp") || lower.contains("water") || lower.contains("drain")) return "MUN";
        if (lower.contains("electric") || lower.contains("bescom") || lower.contains("power")) return "ELE";
        return "POL";
    }

    private String resolveApiKey() {
        if (configuredApiKey != null && !configuredApiKey.isBlank()) {
            return configuredApiKey;
        }
        String key = System.getenv("GEMINI_API_KEY");
        if (key != null && !key.isBlank()) return key;
        key = System.getenv("LLM_API_KEY");
        if (key != null && !key.isBlank()) return key;
        key = System.getenv("OPENAI_API_KEY");
        if (key != null && !key.isBlank()) return key;
        return null;
    }
}
