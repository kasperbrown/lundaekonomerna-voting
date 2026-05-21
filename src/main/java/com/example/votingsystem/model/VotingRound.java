package com.example.votingsystem.model;

import jakarta.persistence.*;

@Entity
public class VotingRound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private int maxSelections;
    private boolean published;
    private boolean closed;

    private String majorityRule;
    private int displayOrder;

    private boolean includeBlank;
    private boolean includeVacant;

    @ManyToOne
    private Meeting meeting;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getMaxSelections() {
        return maxSelections;
    }

    public void setMaxSelections(int maxSelections) {
        this.maxSelections = maxSelections;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public String getMajorityRule() {
        return majorityRule;
    }

    public void setMajorityRule(String majorityRule) {
        this.majorityRule = majorityRule;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isIncludeBlank() {
        return includeBlank;
    }

    public void setIncludeBlank(boolean includeBlank) {
        this.includeBlank = includeBlank;
    }

    public boolean isIncludeVacant() {
        return includeVacant;
    }

    public void setIncludeVacant(boolean includeVacant) {
        this.includeVacant = includeVacant;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    public void setMeeting(Meeting meeting) {
        this.meeting = meeting;
    }
}