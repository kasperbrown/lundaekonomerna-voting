package com.example.votingsystem.model;

import jakarta.persistence.*;

@Entity
public class Alternative {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text;

    private boolean nominated;

    @ManyToOne
    private VotingRound votingRound;

    public Long getId() { return id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isNominated() { return nominated; }
    public void setNominated(boolean nominated) { this.nominated = nominated; }

    public VotingRound getVotingRound() { return votingRound; }
    public void setVotingRound(VotingRound votingRound) { this.votingRound = votingRound; }
}