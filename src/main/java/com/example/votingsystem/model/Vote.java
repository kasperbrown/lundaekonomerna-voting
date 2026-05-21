package com.example.votingsystem.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String voterHash;
    private String voteType;

    @ManyToOne
    private VotingRound votingRound;

    @ManyToMany
    private List<Alternative> selectedAlternatives = new ArrayList<>();

    public Long getId() { return id; }

    public String getVoterHash() { return voterHash; }
    public void setVoterHash(String voterHash) { this.voterHash = voterHash; }

    public String getVoteType() { return voteType; }
    public void setVoteType(String voteType) { this.voteType = voteType; }

    public VotingRound getVotingRound() { return votingRound; }
    public void setVotingRound(VotingRound votingRound) { this.votingRound = votingRound; }

    public List<Alternative> getSelectedAlternatives() { return selectedAlternatives; }
    public void setSelectedAlternatives(List<Alternative> selectedAlternatives) {
        this.selectedAlternatives = selectedAlternatives;
    }
}