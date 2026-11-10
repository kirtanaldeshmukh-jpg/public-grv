package com.resolvetrack.complaint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class TrackingIdGeneratorServiceTest {

    private TrackingIdGeneratorService generatorService;
    private InMemoryTrackingSequenceRepository sequenceRepository;

    @BeforeEach
    void setUp() {
        sequenceRepository = new InMemoryTrackingSequenceRepository();
        generatorService = new TrackingIdGeneratorService(sequenceRepository);
    }

    @Test
    @DisplayName("Tracking ID follows RSK-{year}-{3 digit sequence} format")
    void testGenerateTrackingId_SequentialFormat() {
        String id1 = generatorService.generateTrackingIdForYear(2026);
        String id2 = generatorService.generateTrackingIdForYear(2026);
        String id3 = generatorService.generateTrackingIdForYear(2026);

        assertEquals("RSK-2026-001", id1);
        assertEquals("RSK-2026-002", id2);
        assertEquals("RSK-2026-003", id3);
    }

    @Test
    @DisplayName("Tracking ID resets sequence yearly (new year starts at 001)")
    void testGenerateTrackingId_ResetsYearly() {
        String id2026_1 = generatorService.generateTrackingIdForYear(2026);
        String id2026_2 = generatorService.generateTrackingIdForYear(2026);
        assertEquals("RSK-2026-001", id2026_1);
        assertEquals("RSK-2026-002", id2026_2);

        // Next year starts at 001
        String id2027_1 = generatorService.generateTrackingIdForYear(2027);
        String id2027_2 = generatorService.generateTrackingIdForYear(2027);
        assertEquals("RSK-2027-001", id2027_1);
        assertEquals("RSK-2027-002", id2027_2);
    }

    @Test
    @DisplayName("Tracking ID generation is thread-safe under concurrent execution")
    void testGenerateTrackingId_ThreadSafe() throws InterruptedException, ExecutionException {
        int threadCount = 20;
        int callsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<List<String>>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                List<String> ids = new ArrayList<>();
                for (int j = 0; j < callsPerThread; j++) {
                    ids.add(generatorService.generateTrackingIdForYear(2026));
                }
                return ids;
            }));
        }

        Set<String> uniqueIds = new HashSet<>();
        for (Future<List<String>> f : futures) {
            uniqueIds.addAll(f.get());
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Total 200 calls must produce exactly 200 distinct tracking IDs
        assertEquals(threadCount * callsPerThread, uniqueIds.size());
        assertTrue(uniqueIds.contains("RSK-2026-001"));
        assertTrue(uniqueIds.contains(String.format("RSK-2026-%03d", threadCount * callsPerThread)));
    }

    // =========================================================================
    // In-memory TrackingSequenceRepository
    // =========================================================================

    private static class InMemoryTrackingSequenceRepository implements TrackingSequenceRepository {
        private final Map<Integer, TrackingSequence> store = new ConcurrentHashMap<>();
        private long idGen = 1;

        @Override
        public synchronized Optional<TrackingSequence> findByYearForUpdate(int year) {
            return Optional.ofNullable(store.get(year));
        }

        @Override
        public Optional<TrackingSequence> findByYear(int year) {
            return Optional.ofNullable(store.get(year));
        }

        @Override
        public synchronized <S extends TrackingSequence> S saveAndFlush(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            store.put(entity.getYear(), entity);
            return entity;
        }

        @Override
        public synchronized <S extends TrackingSequence> S save(S entity) {
            return saveAndFlush(entity);
        }

        @Override public void flush() {}
        @Override public <S extends TrackingSequence> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<TrackingSequence> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public TrackingSequence getOne(Long aLong) { return null; }
        @Override public TrackingSequence getById(Long aLong) { return null; }
        @Override public TrackingSequence getReferenceById(Long aLong) { return null; }
        @Override public <S extends TrackingSequence> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends TrackingSequence> List<S> findAll(org.springframework.data.domain.Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends TrackingSequence> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public <S extends TrackingSequence> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends TrackingSequence> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends TrackingSequence> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends TrackingSequence, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends TrackingSequence> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public Optional<TrackingSequence> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<TrackingSequence> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<TrackingSequence> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(TrackingSequence entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends TrackingSequence> entities) {}
        @Override public void deleteAll() {}
        @Override public List<TrackingSequence> findAll(org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public org.springframework.data.domain.Page<TrackingSequence> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
    }
}

