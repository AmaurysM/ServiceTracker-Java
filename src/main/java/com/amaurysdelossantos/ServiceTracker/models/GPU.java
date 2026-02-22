package com.amaurysdelossantos.ServiceTracker.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GPU {
    private String id;
    private Double hours;             // nullable
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;      // nullable
    private String completedBy;      // nullable
    private Instant deletedAt;       // nullable
}
