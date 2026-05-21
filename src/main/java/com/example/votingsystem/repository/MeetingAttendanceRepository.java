package com.example.votingsystem.repository;

import com.example.votingsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingAttendanceRepository extends JpaRepository<MeetingAttendance, Long> {
    List<MeetingAttendance> findByMeeting(Meeting meeting);
    Optional<MeetingAttendance> findByMeetingAndMember(Meeting meeting, Member member);
}