package com.resolvetrack.department;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DepartmentSuggestionServiceTest {

    private RuleBasedDepartmentSuggestionService suggestionService;
    private InMemoryDepartmentRepository departmentRepository;

    @BeforeEach
    void setUp() {
        departmentRepository = new InMemoryDepartmentRepository();
        suggestionService = new RuleBasedDepartmentSuggestionService(departmentRepository);

        departmentRepository.save(Department.builder().id(1L).code("POL").nameEn("Police Department").nameKn("ಪೊಲೀಸ್ ಇಲಾಖೆ").build());
        departmentRepository.save(Department.builder().id(2L).code("CYB").nameEn("Cybercrime Department").nameKn("ಸೈಬರ್ ಅಪರಾಧ ಇಲಾಖೆ").build());
        departmentRepository.save(Department.builder().id(3L).code("PWD").nameEn("Public Works Department").nameKn("ಲೋಕೋಪಯೋಗಿ ಇಲಾಖೆ").build());
        departmentRepository.save(Department.builder().id(4L).code("MUN").nameEn("Municipal Administration (BBMP)").nameKn("ನಗರ ಆಡಳಿತ (ಬಿಬಿಎಂಪಿ)").build());
        departmentRepository.save(Department.builder().id(5L).code("ELE").nameEn("Electricity Department (BESCOM)").nameKn("ವಿದ್ಯುತ್ ಸರಬರಾಜು ಇಲಾಖೆ (ಬೆಸ್ಕಾಂ)").build());
    }

    @Test
    @DisplayName("English keywords: fraud, online, UPI -> Cybercrime")
    void testSuggestDepartment_Cybercrime_English() {
        DepartmentSuggestionService.SuggestionResult result = suggestionService.suggestDepartment(
                "Financial Fraud",
                "I was a victim of an online UPI fraud and lost money through phishing link."
        );

        assertNotNull(result);
        assertEquals("CYB", result.getDepartmentCode());
        assertEquals("Cybercrime Department", result.getDepartmentNameEn());
        assertTrue(result.getConfidence() >= 0.85);
        assertTrue(result.getMatchedKeywords().contains("fraud"));
        assertTrue(result.getMatchedKeywords().contains("online"));
        assertTrue(result.getMatchedKeywords().contains("upi"));
    }

    @Test
    @DisplayName("Kannada keywords: ವಂಚನೆ, ಆನ್‌ಲೈನ್, ಯುಪಿಐ -> Cybercrime")
    void testSuggestDepartment_Cybercrime_Kannada() {
        DepartmentSuggestionService.SuggestionResult result = suggestionService.suggestDepartment(
                "ಆನ್‌ಲೈನ್ ವಂಚನೆ",
                "ನನ್ನ ಯುಪಿಐ ಖಾತೆಯಿಂದ ಹಣ ವಂಚನೆ ಮಾಡಲಾಗಿದೆ"
        );

        assertNotNull(result);
        assertEquals("CYB", result.getDepartmentCode());
        assertTrue(result.getConfidence() >= 0.85);
        assertTrue(result.getMatchedKeywords().contains("ವಂಚನೆ"));
        assertTrue(result.getMatchedKeywords().contains("ಆನ್‌ಲೈನ್"));
        assertTrue(result.getMatchedKeywords().contains("ಯುಪಿಐ"));
    }

    @Test
    @DisplayName("English keywords: lost, stolen -> Police")
    void testSuggestDepartment_Police_English() {
        DepartmentSuggestionService.SuggestionResult result = suggestionService.suggestDepartment(
                "Theft",
                "My purse and wallet were stolen at the market, lost my documents."
        );

        assertEquals("POL", result.getDepartmentCode());
        assertEquals("Police Department", result.getDepartmentNameEn());
        assertTrue(result.getMatchedKeywords().contains("stolen") || result.getMatchedKeywords().contains("lost"));
    }

    @Test
    @DisplayName("Kannada keywords: ಕಳ್ಳತನ, ಕಳೆದುಹೋದ -> Police")
    void testSuggestDepartment_Police_Kannada() {
        DepartmentSuggestionService.SuggestionResult result = suggestionService.suggestDepartment(
                "ಕಳ್ಳತನ",
                "ನನ್ನ ಬೈಕ್ ಕಳ್ಳತನವಾಗಿದೆ, ದಾಖಲೆಗಳು ಕಳೆದುಹೋದವು"
        );

        assertEquals("POL", result.getDepartmentCode());
        assertTrue(result.getMatchedKeywords().contains("ಕಳ್ಳತನ") || result.getMatchedKeywords().contains("ಕಳೆದುಹೋದ"));
    }

    @Test
    @DisplayName("English keywords: road, pothole -> Public Works")
    void testSuggestDepartment_PublicWorks_English() {
        DepartmentSuggestionService.SuggestionResult result = suggestionService.suggestDepartment(
                "Road Damage",
                "Deep pothole on state highway road causing accidents near flyover."
        );

        assertEquals("PWD", result.getDepartmentCode());
        assertEquals("Public Works Department", result.getDepartmentNameEn());
        assertTrue(result.getMatchedKeywords().contains("pothole"));
        assertTrue(result.getMatchedKeywords().contains("road"));
    }

    @Test
    @DisplayName("Kannada keywords: ರಸ್ತೆ, ಗುಂಡಿ -> Public Works")
    void testSuggestDepartment_PublicWorks_Kannada() {
        DepartmentSuggestionService.SuggestionResult result = suggestionService.suggestDepartment(
                "ರಸ್ತೆ ಸಮಸ್ಯೆ",
                "ಹೆದ್ದಾರಿ ರಸ್ತೆಯಲ್ಲಿ ದೊಡ್ಡ ಗುಂಡಿ ಬಿದ್ದಿದೆ"
        );

        assertEquals("PWD", result.getDepartmentCode());
        assertTrue(result.getMatchedKeywords().contains("ರಸ್ತೆ") || result.getMatchedKeywords().contains("ಗುಂಡಿ"));
    }

    @Test
    @DisplayName("English keywords: water, drainage -> Municipal")
    void testSuggestDepartment_Municipal_English() {
        DepartmentSuggestionService.SuggestionResult result = suggestionService.suggestDepartment(
                "Civic Sanitation",
                "Stagnant drainage sewage water overflowing on residential street."
        );

        assertEquals("MUN", result.getDepartmentCode());
        assertEquals("Municipal Administration (BBMP)", result.getDepartmentNameEn());
        assertTrue(result.getMatchedKeywords().contains("drainage"));
        assertTrue(result.getMatchedKeywords().contains("water"));
    }

    @Test
    @DisplayName("Kannada keywords: ನೀರು, ಚರಂಡಿ -> Municipal")
    void testSuggestDepartment_Municipal_Kannada() {
        DepartmentSuggestionService.SuggestionResult result = suggestionService.suggestDepartment(
                "ಚರಂಡಿ ನೀರು",
                "ಕುಡಿಯುವ ನೀರು ಕಲುಷಿತವಾಗಿದೆ ಮತ್ತು ಒಳಚರಂಡಿ ತುಂಬಿ ಹರಿಯುತ್ತಿದೆ"
        );

        assertEquals("MUN", result.getDepartmentCode());
        assertTrue(result.getMatchedKeywords().contains("ನೀರು") || result.getMatchedKeywords().contains("ಚರಂಡಿ"));
    }

    @Test
    @DisplayName("English keywords: streetlight, power -> Electricity")
    void testSuggestDepartment_Electricity_English() {
        DepartmentSuggestionService.SuggestionResult result = suggestionService.suggestDepartment(
                "Power Outage",
                "Streetlight not working and power outage due to broken transformer wire."
        );

        assertEquals("ELE", result.getDepartmentCode());
        assertEquals("Electricity Department (BESCOM)", result.getDepartmentNameEn());
        assertTrue(result.getMatchedKeywords().contains("streetlight") || result.getMatchedKeywords().contains("power"));
    }

    @Test
    @DisplayName("Kannada keywords: ಬೀದಿ ದೀಪ, ವಿದ್ಯುತ್ -> Electricity")
    void testSuggestDepartment_Electricity_Kannada() {
        DepartmentSuggestionService.SuggestionResult result = suggestionService.suggestDepartment(
                "ವಿದ್ಯುತ್ ಸಮಸ್ಯೆ",
                "ಬೀದಿ ದೀಪ ಉರಿಯುತ್ತಿಲ್ಲ ಮತ್ತು ಟ್ರಾನ್ಸ್‌ಫಾರ್ಮರ್ ವಿದ್ಯುತ್ ಕಡಿತವಾಗಿದೆ"
        );

        assertEquals("ELE", result.getDepartmentCode());
        assertTrue(result.getMatchedKeywords().contains("ವಿದ್ಯುತ್") || result.getMatchedKeywords().contains("ಬೀದಿ ದೀಪ"));
    }

    // =========================================================================
    // In-memory DepartmentRepository
    // =========================================================================

    private static class InMemoryDepartmentRepository implements DepartmentRepository {
        private final Map<String, Department> byCode = new HashMap<>();
        private final Map<Long, Department> byId = new HashMap<>();

        @Override
        public Optional<Department> findByCode(String code) {
            return Optional.ofNullable(byCode.get(code));
        }

        @Override
        public <S extends Department> S save(S entity) {
            byCode.put(entity.getCode(), entity);
            byId.put(entity.getId(), entity);
            return entity;
        }

        @Override public List<Department> findAllByActiveTrue() { return new ArrayList<>(byId.values()); }
        @Override public Optional<Department> findById(Long aLong) { return Optional.ofNullable(byId.get(aLong)); }
        @Override public void flush() {}
        @Override public <S extends Department> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Department> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<Department> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Department getOne(Long aLong) { return null; }
        @Override public Department getById(Long aLong) { return null; }
        @Override public Department getReferenceById(Long aLong) { return null; }
        @Override public <S extends Department> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends Department> List<S> findAll(org.springframework.data.domain.Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Department> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Department> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends Department> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends Department> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends Department, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Department> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<Department> findAll() { return new ArrayList<>(byId.values()); }
        @Override public List<Department> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return byId.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Department entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Department> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Department> findAll(org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public org.springframework.data.domain.Page<Department> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
    }
}
