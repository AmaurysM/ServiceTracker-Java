package com.amaurysdelossantos.ServiceTracker.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WindshieldCleaning {
    private String id;
    private String note;             // nullable
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;     // nullable
    private String completedBy;       // nullable
    private Instant deletedAt;        // nullable
}
