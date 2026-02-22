package com.amaurysdelossantos.ServiceTracker.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "serviceitems")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItem {
    @Id
    private String id;

    private String companyId;                // nullable
    private String tail;
    private Fuel fuel;                        // nullable
    private List<Catering> catering;          // nullable
    private GPU gpu;                          // nullable
    private Lavatory lavatory;                // nullable
    private PotableWater potableWater;        // nullable
    private WindshieldCleaning windshieldCleaning; // nullable
    private OilService oilService;            // nullable
    private Instant arrival;                 // nullable
    private Instant departure;                // nullable
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;              // nullable
    private String completedBy;               // nullable
    private Instant deletedAt;                // nullable
    private Map<String, Object> metadata;      // nullable
 }
