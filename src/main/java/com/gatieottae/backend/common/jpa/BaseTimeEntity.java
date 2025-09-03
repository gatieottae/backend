package com.gatieottae.backend.common.jpa;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

    @Column(name = "created_at", nullable = false)
    protected OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    protected OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}