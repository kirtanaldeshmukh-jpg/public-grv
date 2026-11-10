package com.resolvetrack.department;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiSuggestionServiceTest {

    @Mock
    private RuleBasedDepartmentSuggestionService ruleBasedService;

    @Mock
    private DepartmentRepository departmentRepository;

    private AiSuggestionService aiSuggestionService;
    private Department municipalDept;

    @BeforeEach
    void setUp() {
        aiSuggestionService = new AiSuggestionService(
                ruleBasedService,
                departmentRepository,
                new ObjectMapper()
        );

        municipalDept = new Department();
        municipalDept.setId(4L);
        municipalDept.setCode("MUN");
        municipalDept.setNameEn("Municipal Administration (BBMP)");
        municipalDept.setNameKn("ನಗರ ಆಡಳಿತ");
    }

    @Test
    @DisplayName("Prompt 8.2: When no LLM API key is configured, seamlessly falls back to rule-based suggester")
    void testFallbackWhenNoApiKey() {
        DepartmentSuggestionService.SuggestionResult ruleFallback = DepartmentSuggestionService.SuggestionResult.builder()
                .departmentId(4L)
                .departmentCode("MUN")
                .confidence(0.88)
                .source("RULE_BASED")
                .explanation("Rule engine matched keyword: water")
                .build();

        when(ruleBasedService.suggestDepartment(anyString(), anyString())).thenReturn(ruleFallback);

        DepartmentSuggestionService.SuggestionResult result = aiSuggestionService.suggestDepartment(
                "Water & Drainage",
                "Severe Cauvery water pipeline burst on 8th main road"
        );

        assertThat(result).isNotNull();
        assertThat(result.getDepartmentCode()).isEqualTo("MUN");
        assertThat(result.getSource()).isEqualTo("RULE_BASED");
        verify(ruleBasedService, times(1)).suggestDepartment(anyString(), anyString());
    }

    @Test
    @DisplayName("Prompt 8.2: Validates JSON structure and fields returned from LLM call")
    void testParseAndValidateJsonSuccess() {
        when(departmentRepository.findByCode("MUN")).thenReturn(Optional.of(municipalDept));

        String rawLlmResponse = "```json\n" +
                "{\n" +
                "  \"category\": \"Water & Sanitation\",\n" +
                "  \"department\": \"MUN\",\n" +
                "  \"priority\": \"HIGH\",\n" +
                "  \"issueSummary\": \"Major pipeline rupture on 8th Main Vidyaranyapura causing potable water flooding\",\n" +
                "  \"extractedLocation\": \"8th Main Road, Vidyaranyapura, Bengaluru\",\n" +
                "  \"confidence\": 0.94\n" +
                "}\n" +
                "```";

        DepartmentSuggestionService.SuggestionResult result = aiSuggestionService.parseAndValidateJson(
                rawLlmResponse, "Water & Drainage", "text"
        );

        assertThat(result).isNotNull();
        assertThat(result.getSource()).isEqualTo("AI");
        assertThat(result.getDepartmentId()).isEqualTo(4L);
        assertThat(result.getDepartmentCode()).isEqualTo("MUN");
        assertThat(result.getConfidence()).isEqualTo(0.94);
        assertThat(result.getPriority()).isEqualTo("HIGH");
        assertThat(result.getIssueSummary()).contains("Major pipeline rupture");
        assertThat(result.getExtractedLocation()).isEqualTo("8th Main Road, Vidyaranyapura, Bengaluru");
        assertThat(result.getExplanation()).contains("AI-assisted classification");
    }

    @Test
    @DisplayName("Prompt 8.2: Invalid JSON or unknown department from LLM falls back to rule-based")
    void testInvalidJsonFallsBackToRuleBased() {
        DepartmentSuggestionService.SuggestionResult ruleFallback = DepartmentSuggestionService.SuggestionResult.builder()
                .departmentId(1L)
                .departmentCode("POL")
                .confidence(0.75)
                .source("RULE_BASED")
                .build();

        when(ruleBasedService.suggestDepartment("Cybercrime", "unparseable response")).thenReturn(ruleFallback);

        String invalidLlmOutput = "This is not JSON at all, just plain conversational refusal.";

        DepartmentSuggestionService.SuggestionResult result = aiSuggestionService.parseAndValidateJson(
                invalidLlmOutput, "Cybercrime", "unparseable response"
        );

        assertThat(result).isNotNull();
        assertThat(result.getSource()).isEqualTo("RULE_BASED");
        verify(ruleBasedService, times(1)).suggestDepartment("Cybercrime", "unparseable response");
    }
}

