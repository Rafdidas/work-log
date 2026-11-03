package com.worklog.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "team_label", nullable = false)
    private String teamLabel = "개발1팀";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {}

    public User(String email, String passwordHash, String name, Instant createdAt) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public String getTeamLabel() {
        return teamLabel;
    }

    public void setTeamLabel(String teamLabel) {
        this.teamLabel = teamLabel;
    }
}
