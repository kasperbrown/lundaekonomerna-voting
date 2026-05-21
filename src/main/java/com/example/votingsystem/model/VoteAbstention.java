package com.example.votingsystem.model;

import jakarta.persistence.*;

@Entity
public class VoteAbstention {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private VotingRound votingRound;

    @ManyToOne
    private Member member;

    public Long getId() { return id; }

    public VotingRound getVotingRound() { return votingRound; }
    public void setVotingRound(VotingRound votingRound) { this.votingRound = votingRound; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
}