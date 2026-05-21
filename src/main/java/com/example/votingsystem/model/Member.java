package com.example.votingsystem.model;

import jakarta.persistence.*;

@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String role;

    @Transient
    private String meetingVotingCode;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMeetingVotingCode() {
        return meetingVotingCode;
    }

    public void setMeetingVotingCode(String meetingVotingCode) {
        this.meetingVotingCode = meetingVotingCode;
    }
}