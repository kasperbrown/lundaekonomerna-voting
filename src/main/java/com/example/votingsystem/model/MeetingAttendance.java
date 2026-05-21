package com.example.votingsystem.model;

import jakarta.persistence.*;

@Entity
public class MeetingAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean present;

    @ManyToOne
    private Meeting meeting;

    @ManyToOne
    private Member member;

    public Long getId() { return id; }

    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }

    public Meeting getMeeting() { return meeting; }
    public void setMeeting(Meeting meeting) { this.meeting = meeting; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
}