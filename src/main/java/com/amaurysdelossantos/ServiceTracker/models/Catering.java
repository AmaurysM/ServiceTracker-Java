package com.amaurysdelossantos.ServiceTracker.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Catering {
    private String id;
    private String itemId;
    private Integer cateringNumber;   // nullable
    private String note;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;      // nullable
    private String completedBy;       // nullable
    private Instant deletedAt;        // nullable
}