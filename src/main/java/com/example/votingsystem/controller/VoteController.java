package com.example.votingsystem.controller;

import com.example.votingsystem.model.*;
import com.example.votingsystem.repository.*;
import com.example.votingsystem.service.VotingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Controller
public class VoteController {

    private final VotingRoundRepository votingRoundRepository;
    private final AlternativeRepository alternativeRepository;
    private final VoteRepository voteRepository;
    private final MemberRepository memberRepository;
    private final MeetingAttendanceRepository meetingAttendanceRepository;
    private final VoteAbstentionRepository voteAbstentionRepository;
    private final VotingService votingService;
    private final MeetingVotingCodeRepository meetingVotingCodeRepository;

    public VoteController(
            VotingRoundRepository votingRoundRepository,
            AlternativeRepository alternativeRepository,
            VoteRepository voteRepository,
            MemberRepository memberRepository,
            MeetingAttendanceRepository meetingAttendanceRepository,
            VoteAbstentionRepository voteAbstentionRepository,
            VotingService votingService,
            MeetingVotingCodeRepository meetingVotingCodeRepository) {

        this.votingRoundRepository = votingRoundRepository;
        this.alternativeRepository = alternativeRepository;
        this.voteRepository = voteRepository;
        this.memberRepository = memberRepository;
        this.meetingAttendanceRepository = meetingAttendanceRepository;
        this.voteAbstentionRepository = voteAbstentionRepository;
        this.votingService = votingService;
        this.meetingVotingCodeRepository = meetingVotingCodeRepository;
    }

    private String createVoterHash(Long roundId, Long memberId) {
        try {
            String input = "round:" + roundId + ":member:" + memberId;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Could not create voter hash", e);
        }
    }

    @GetMapping("/vote")
    public String openVotesPage(Model model) {
        List<VotingRound> openVotes = votingRoundRepository
                .findByPublishedTrueAndClosedFalseOrderByDisplayOrderAscIdAsc();

        model.addAttribute("openVotes", openVotes);
        model.addAttribute("hasOpenVotes", !openVotes.isEmpty());

        return "vote-list";
    }

    @GetMapping("/vote/{roundId}")
    public String votePage(@PathVariable Long roundId, Model model) {
        Optional<VotingRound> roundOptional = votingRoundRepository.findById(roundId);

        if (roundOptional.isEmpty() || !roundOptional.get().isPublished()) {
            return "redirect:/vote";
        }

        VotingRound round = roundOptional.get();

        model.addAttribute("round", round);
        model.addAttribute("alternatives", alternativeRepository.findByVotingRound(round));
        model.addAttribute("success", false);

        return "vote";
    }

    @PostMapping("/vote/{roundId}")
    public String submitVote(
            @PathVariable Long roundId,
            @RequestParam String votingCode,
            @RequestParam(required = false) String specialVote,
            @RequestParam(required = false) List<Long> alternativeIds,
            Model model) {

        Optional<VotingRound> roundOptional = votingRoundRepository.findById(roundId);

        if (roundOptional.isEmpty() || !roundOptional.get().isPublished()) {
            return "redirect:/vote";
        }

        VotingRound round = roundOptional.get();
        List<Alternative> alternatives = alternativeRepository.findByVotingRound(round);

        if (round.isClosed()) {
            model.addAttribute("error", "Voting has ended.");
            model.addAttribute("round", round);
            model.addAttribute("alternatives", alternatives);
            model.addAttribute("success", false);
            return "vote";
        }

        List<Member> ordinaryMembers = memberRepository.findByRoleOrderByIdAsc("ORDINARY");
        List<Member> deputyMembers = memberRepository.findByRoleOrderByIdAsc("DEPUTY");

        List<MeetingAttendance> attendanceList = meetingAttendanceRepository.findByMeeting(round.getMeeting());
        List<VoteAbstention> abstentions = voteAbstentionRepository.findByVotingRound(round);

        List<Member> eligibleVoters = votingService.getEligibleVotersForRound(
                ordinaryMembers,
                deputyMembers,
                attendanceList,
                abstentions);

        Optional<Member> memberOptional;

        if ("A".equalsIgnoreCase(votingCode.trim())) {
            memberOptional = eligibleVoters.stream()
                    .filter(member -> voteRepository
                            .findByVotingRoundAndVoterHash(
                                    round,
                                    createVoterHash(round.getId(), member.getId()))
                            .isEmpty())
                    .findFirst();
        } else {
            Optional<MeetingVotingCode> codeOptional = meetingVotingCodeRepository
                    .findByCodeIgnoreCase(votingCode.trim());

            if (codeOptional.isEmpty()
                    || !codeOptional.get().getMeeting().getId().equals(round.getMeeting().getId())) {
                memberOptional = Optional.empty();
            } else {
                memberOptional = Optional.of(codeOptional.get().getMember());
            }
        }

        if (memberOptional.isEmpty()) {
            model.addAttribute("error", "Invalid code, or all eligible test voters have already voted.");
            model.addAttribute("round", round);
            model.addAttribute("alternatives", alternatives);
            model.addAttribute("success", false);
            return "vote";
        }

        Member member = memberOptional.get();

        boolean isEligible = eligibleVoters.stream()
                .anyMatch(eligible -> eligible.getId().equals(member.getId()));

        if (!isEligible) {
            model.addAttribute("error", "This voting code is not eligible to vote in this round.");
            model.addAttribute("round", round);
            model.addAttribute("alternatives", alternatives);
            model.addAttribute("success", false);
            return "vote";
        }

        String voterHash = createVoterHash(round.getId(), member.getId());

        if (voteRepository.findByVotingRoundAndVoterHash(round, voterHash).isPresent()) {
            model.addAttribute("error", "This voter has already voted in this round.");
            model.addAttribute("round", round);
            model.addAttribute("alternatives", alternatives);
            model.addAttribute("success", false);
            return "vote";
        }

        boolean hasAlternativeSelection = alternativeIds != null && !alternativeIds.isEmpty();
        boolean hasSpecialVote = specialVote != null && !specialVote.isBlank();

        if (hasSpecialVote && hasAlternativeSelection) {
            model.addAttribute("error", "Please choose either a candidate, blank, or vacant — not several types.");
            model.addAttribute("round", round);
            model.addAttribute("alternatives", alternatives);
            model.addAttribute("success", false);
            return "vote";
        }

        Vote vote = new Vote();
        vote.setVoterHash(voterHash);
        vote.setVotingRound(round);

        if ("BLANK".equals(specialVote)) {
            if (!round.isIncludeBlank()) {
                model.addAttribute("error", "Blank voting is not available for this vote.");
                model.addAttribute("round", round);
                model.addAttribute("alternatives", alternatives);
                model.addAttribute("success", false);
                return "vote";
            }

            vote.setVoteType("BLANK");
            vote.setSelectedAlternatives(new ArrayList<>());

        } else if ("VACANT".equals(specialVote)) {
            if (!round.isIncludeVacant()) {
                model.addAttribute("error", "Vacant voting is not available for this vote.");
                model.addAttribute("round", round);
                model.addAttribute("alternatives", alternatives);
                model.addAttribute("success", false);
                return "vote";
            }

            vote.setVoteType("VACANT");
            vote.setSelectedAlternatives(new ArrayList<>());

        } else {
            if (!hasAlternativeSelection) {
                model.addAttribute("error", "Please select at least one candidate.");
                model.addAttribute("round", round);
                model.addAttribute("alternatives", alternatives);
                model.addAttribute("success", false);
                return "vote";
            }

            if (alternativeIds.size() > round.getMaxSelections()) {
                model.addAttribute("error", "You selected too many candidates.");
                model.addAttribute("round", round);
                model.addAttribute("alternatives", alternatives);
                model.addAttribute("success", false);
                return "vote";
            }

            vote.setVoteType("NORMAL");
            vote.setSelectedAlternatives(alternativeRepository.findAllById(alternativeIds));
        }

        voteRepository.save(vote);

        model.addAttribute("round", round);
        model.addAttribute("alternatives", alternatives);
        model.addAttribute("success", true);

        return "vote";
    }

    @GetMapping("/results/{roundId}")
    public String results(@PathVariable Long roundId, Model model) {

        VotingRound round = votingRoundRepository.findById(roundId).orElseThrow();

        List<Member> ordinaryMembers = memberRepository.findByRoleOrderByIdAsc("ORDINARY");
        List<Member> deputyMembers = memberRepository.findByRoleOrderByIdAsc("DEPUTY");

        List<Member> allMembers = new ArrayList<>();
        allMembers.addAll(ordinaryMembers);
        allMembers.addAll(deputyMembers);

        for (MeetingVotingCode code : meetingVotingCodeRepository.findByMeeting(round.getMeeting())) {
            if (code.getMember() != null) {
                for (Member member : allMembers) {
                    if (member.getId().equals(code.getMember().getId())) {
                        member.setMeetingVotingCode(code.getCode());
                    }
                }
            }
        }

        List<MeetingAttendance> attendanceList = meetingAttendanceRepository.findByMeeting(round.getMeeting());
        List<Alternative> alternatives = alternativeRepository.findByVotingRound(round);
        List<Vote> votes = voteRepository.findByVotingRound(round);
        List<VoteAbstention> abstentions = voteAbstentionRepository.findByVotingRound(round);

        model.addAttribute("round", round);
        model.addAttribute("results", votingService.calculateResults(
                round,
                ordinaryMembers,
                deputyMembers,
                attendanceList,
                alternatives,
                votes,
                abstentions));

        return "results";
    }
}