package com.example.votingsystem.repository;

import com.example.votingsystem.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByRole(String role);

    List<Member> findByRoleOrderByIdAsc(String role);

    Optional<Member> findByNameIgnoreCase(String name);
}