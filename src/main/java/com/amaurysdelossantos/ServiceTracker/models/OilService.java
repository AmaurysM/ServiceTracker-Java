package com.amaurysdelossantos.ServiceTracker.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OilService {
    private String id;
    private String type;
    private double quarts;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;      // nullable
    private String completedBy;      // nullable
    private Instant deletedA;         // nullable
}