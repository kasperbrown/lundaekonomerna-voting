package com.example.votingsystem.repository;

import com.example.votingsystem.model.Vote;
import com.example.votingsystem.model.VotingRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    List<Vote> findByVotingRound(VotingRound votingRound);
    Optional<Vote> findByVotingRoundAndVoterHash(VotingRound votingRound, String voterHash);
}