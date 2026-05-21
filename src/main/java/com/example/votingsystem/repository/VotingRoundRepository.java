package com.example.votingsystem.repository;

import com.example.votingsystem.model.Meeting;
import com.example.votingsystem.model.VotingRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VotingRoundRepository extends JpaRepository<VotingRound, Long> {
    List<VotingRound> findByMeeting(Meeting meeting);

    List<VotingRound> findByMeetingOrderByDisplayOrderAscIdAsc(Meeting meeting);

    List<VotingRound> findByPublishedTrue();
}