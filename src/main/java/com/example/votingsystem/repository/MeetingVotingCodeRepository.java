package com.example.votingsystem.repository;

import com.example.votingsystem.model.Meeting;
import com.example.votingsystem.model.MeetingVotingCode;
import com.example.votingsystem.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingVotingCodeRepository extends JpaRepository<MeetingVotingCode, Long> {

    Optional<MeetingVotingCode> findByCodeIgnoreCase(String code);

    Optional<MeetingVotingCode> findByMeetingAndMember(Meeting meeting, Member member);

    List<MeetingVotingCode> findByMeeting(Meeting meeting);
}