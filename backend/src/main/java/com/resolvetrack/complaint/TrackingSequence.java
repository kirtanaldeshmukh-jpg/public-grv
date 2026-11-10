package com.resolvetrack.complaint;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_sequences", indexes = {
        @Index(name = "idx_tracking_year", columnList = "sequence_year")
})
public class TrackingSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequence_year", nullable = false, unique = true)
    private Integer year;

    @Column(name = "current_sequence", nullable = false)
    private Long currentSequence = 0L;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TrackingSequence() {}

    public TrackingSequence(Long id, Integer year, Long currentSequence, LocalDateTime updatedAt) {
        this.id = id;
        this.year = year;
        this.currentSequence = currentSequence != null ? currentSequence : 0L;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Long getCurrentSequence() {
        return currentSequence;
    }

    public void setCurrentSequence(Long currentSequence) {
        this.currentSequence = currentSequence;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
