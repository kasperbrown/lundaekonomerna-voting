package com.example.votingsystem.model;

import jakarta.persistence.*;

@Entity
public class Attendee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String role; // ORDINARY or DEPUTY
    private boolean present;
    private boolean abstaining;

    @ManyToOne
    private Meeting meeting;

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }

    public boolean isAbstaining() { return abstaining; }
    public void setAbstaining(boolean abstaining) { this.abstaining = abstaining; }

    public Meeting getMeeting() { return meeting; }
    public void setMeeting(Meeting meeting) { this.meeting = meeting; }
}