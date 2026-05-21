package com.example.votingsystem.repository;

import com.example.votingsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoteAbstentionRepository extends JpaRepository<VoteAbstention, Long> {
    List<VoteAbstention> findByVotingRound(VotingRound votingRound);
    Optional<VoteAbstention> findByVotingRoundAndMember(VotingRound votingRound, Member member);
}