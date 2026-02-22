package com.amaurysdelossantos.ServiceTracker.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fuel {
    private String id;
    private String tail;
    private String type;
    private Double gallons;         // nullable
    private double weight;
    private String note;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;      // nullable
    private String completedBy;       // nullable
    private Instant deletedAt;

}
