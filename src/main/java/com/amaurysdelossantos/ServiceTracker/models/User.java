package com.amaurysdelossantos.ServiceTracker.models;

import com.amaurysdelossantos.ServiceTracker.models.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;                        // maps to MongoDB _id
    private String firstname;
    private String lastname;
    private String email;
    private String username;
    private Role role;
    private List<String> modules;             // ObjectIds as strings
    private boolean deleted;
    private boolean archived;
    private boolean invited;
    private String inviteToken;              // nullable
    private Instant inviteExpiresAt;         // nullable
    private String password;                  // nullable
    private String nickname;                  // nullable
    private String companyId;                 // nullable
    private Instant deletedAt;                // nullable
    private Instant archivedAt;               // nullable
    private Instant createdAt;
    private Instant updatedAt;
}