package com.example.votingsystem.repository;

import com.example.votingsystem.model.Alternative;
import com.example.votingsystem.model.VotingRound;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlternativeRepository extends JpaRepository<Alternative, Long> {
    List<Alternative> findByVotingRound(VotingRound votingRound);
}