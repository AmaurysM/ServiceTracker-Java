package com.amaurysdelossantos.ServiceTracker.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plane {
    private String id;
    private String tail;
    private String type;
    private String note;
    private boolean deleted;
    private Instant deletedAt;        // nullable
}