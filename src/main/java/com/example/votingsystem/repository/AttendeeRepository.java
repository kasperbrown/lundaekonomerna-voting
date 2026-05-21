package com.example.votingsystem.repository;

import com.example.votingsystem.model.Attendee;
import com.example.votingsystem.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttendeeRepository extends JpaRepository<Attendee, Long> {
    List<Attendee> findByMeeting(Meeting meeting);
}