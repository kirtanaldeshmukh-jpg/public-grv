package com.resolvetrack.department;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RuleBasedDepartmentSuggestionService implements DepartmentSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedDepartmentSuggestionService.class);

    private final DepartmentRepository departmentRepository;

    private static final Map<String, List<String>> KEYWORD_RULES = new LinkedHashMap<>();

    static {
        // 1. Cybercrime keywords (English & Kannada)
        KEYWORD_RULES.put("CYB", List.of(
                "fraud", "online", "upi", "phishing", "scam", "hacked", "hack", "otp", "cyber",
                "cybercrime", "bank fraud", "credit card", "debit card", "crypto", "bitcoin",
                "ವಂಚನೆ", "ಆನ್‌ಲೈನ್", "ಯುಪಿಐ", "ಸೈಬರ್", "ಹ್ಯಾಕ್", "ಓಟಿಪಿ", "ಖಾತೆ"
        ));

        // 2. Police keywords (English & Kannada)
        KEYWORD_RULES.put("POL", List.of(
                "lost", "stolen", "theft", "robbery", "assault", "harassment", "threat", "patrol",
                "fir", "police", "missing", "burglary", "violence", "purse", "wallet", "chain snatching",
                "ಕಳೆದುಹೋದ", "ಕಳ್ಳತನ", "ದರೋಡೆ", "ಹಲ್ಲೆ", "ಬೆದರಿಕೆ", "ಪೊಲೀಸ್", "ನಾಪತ್ತೆ"
        ));

        // 3. Public Works keywords (English & Kannada)
        KEYWORD_RULES.put("PWD", List.of(
                "road", "pothole", "bridge", "flyover", "highway", "tarmac", "footpath", "pavement",
                "construction", "asphalt", "culvert", "state highway", "bypass",
                "ರಸ್ತೆ", "ಗುಂಡಿ", "ಸೇತುವೆ", "ಹೆದ್ದಾರಿ", "ಕಾಮಗಾರಿ", "ಕಾಲುದಾರಿ"
        ));

        // 4. Municipal Administration keywords (English & Kannada)
        KEYWORD_RULES.put("MUN", List.of(
                "water", "drainage", "garbage", "waste", "sewage", "sanitation", "bbmp",
                "municipality", "park", "leakage", "drinking water", "overflow", "gutter", "manhole",
                "ನೀರು", "ಚರಂಡಿ", "ಕಸ", "ತ್ಯಾಜ್ಯ", "ನೈರ್ಮಲ್ಯ", "ಪಾಲಿಕೆ", "ಒಳಚರಂಡಿ", "ಕುಡಿಯುವ ನೀರು"
        ));

        // 5. Electricity keywords (English & Kannada)
        KEYWORD_RULES.put("ELE", List.of(
                "streetlight", "street light", "power", "electricity", "transformer", "blackout",
                "bescom", "outage", "meter", "voltage", "wire", "spark", "electric wire", "high tension",
                "ಬೀದಿ ದೀಪ", "ವಿದ್ಯುತ್", "ಟ್ರಾನ್ಸ್‌ಫಾರ್ಮರ್", "ಕರೆಂಟ್", "ಬೆಸ್ಕಾಂ", "ವಿದ್ಯುತ್ ಕಡಿತ", "ಮೀಟರ್"
        ));
    }

    public RuleBasedDepartmentSuggestionService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public SuggestionResult suggestDepartment(String category, String text) {
        String combined = (category != null ? category : "") + " " + (text != null ? text : "");
        String normalized = combined.toLowerCase(Locale.ROOT);

        String bestDeptCode = "POL"; // Baseline fallback
        int maxMatches = 0;
        List<String> bestMatchedKeywords = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : KEYWORD_RULES.entrySet()) {
            String deptCode = entry.getKey();
            List<String> keywords = entry.getValue();

            List<String> matched = new ArrayList<>();
            for (String kw : keywords) {
                if (normalized.contains(kw.toLowerCase(Locale.ROOT))) {
                    matched.add(kw);
                }
            }

            if (matched.size() > maxMatches) {
                maxMatches = matched.size();
                bestDeptCode = deptCode;
                bestMatchedKeywords = matched;
            }
        }

        double confidence;
        if (maxMatches == 0) {
            confidence = 0.20;
        } else if (maxMatches == 1) {
            confidence = 0.75;
        } else if (maxMatches == 2) {
            confidence = 0.88;
        } else {
            confidence = 0.96;
        }

        // Fetch department metadata
        Optional<Department> deptOpt = departmentRepository.findByCode(bestDeptCode);
        Long deptId = deptOpt.map(Department::getId).orElse(1L);
        String nameEn = deptOpt.map(Department::getNameEn).orElse("Police Department");
        String nameKn = deptOpt.map(Department::getNameKn).orElse("ಪೊಲೀಸ್ ಇಲಾಖೆ");

        String explanation = maxMatches > 0
                ? String.format("Rule engine matched %d keyword(s): %s", maxMatches, String.join(", ", bestMatchedKeywords))
                : "No decisive keywords matched; defaulted to civic police assistance";

        log.debug("Department suggestion: code={}, confidence={}, matched={}", bestDeptCode, confidence, bestMatchedKeywords);

        return SuggestionResult.builder()
                .departmentId(deptId)
                .departmentCode(bestDeptCode)
                .departmentNameEn(nameEn)
                .departmentNameKn(nameKn)
                .confidence(confidence)
                .matchedKeywords(bestMatchedKeywords)
                .explanation(explanation)
                .build();
    }
}

