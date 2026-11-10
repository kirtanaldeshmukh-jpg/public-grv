package com.resolvetrack.complaint;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrackingSequenceRepository extends JpaRepository<TrackingSequence, Long> {

    @Query("SELECT s FROM TrackingSequence s WHERE s.year = :year")
    Optional<TrackingSequence> findByYearForUpdate(@Param("year") int year);

    Optional<TrackingSequence> findByYear(int year);
}

