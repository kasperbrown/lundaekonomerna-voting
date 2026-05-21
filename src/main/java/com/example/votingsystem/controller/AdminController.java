package com.example.votingsystem.controller;

import com.example.votingsystem.model.*;
import com.example.votingsystem.repository.*;
import com.example.votingsystem.service.VotingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final MeetingRepository meetingRepository;
    private final MeetingVotingCodeRepository meetingVotingCodeRepository;
    private final MemberRepository memberRepository;
    private final MeetingAttendanceRepository meetingAttendanceRepository;
    private final VotingRoundRepository votingRoundRepository;
    private final AlternativeRepository alternativeRepository;
    private final VoteAbstentionRepository voteAbstentionRepository;
    private final VoteRepository voteRepository;
    private final VotingService votingService;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public AdminController(
            MeetingRepository meetingRepository,
            MemberRepository memberRepository,
            MeetingAttendanceRepository meetingAttendanceRepository,
            VotingRoundRepository votingRoundRepository,
            AlternativeRepository alternativeRepository,
            VoteAbstentionRepository voteAbstentionRepository,
            VoteRepository voteRepository,
            VotingService votingService,
            MeetingVotingCodeRepository meetingVotingCodeRepository) {

        this.meetingRepository = meetingRepository;
        this.memberRepository = memberRepository;
        this.meetingAttendanceRepository = meetingAttendanceRepository;
        this.votingRoundRepository = votingRoundRepository;
        this.alternativeRepository = alternativeRepository;
        this.voteAbstentionRepository = voteAbstentionRepository;
        this.voteRepository = voteRepository;
        this.votingService = votingService;
        this.meetingVotingCodeRepository = meetingVotingCodeRepository;
    }

    private String generateUniqueVotingCode() {

        String code;

        do {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < 6; i++) {
                builder.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }

            code = builder.toString();

        } while (meetingVotingCodeRepository.findByCodeIgnoreCase(code).isPresent());

        return code;
    }

    private void ensureMeetingVotingCodesExist(Meeting meeting) {

        for (Member member : memberRepository.findAll()) {

            Optional<MeetingVotingCode> existing = meetingVotingCodeRepository.findByMeetingAndMember(meeting, member);

            if (existing.isEmpty()) {

                MeetingVotingCode code = new MeetingVotingCode();

                code.setMeeting(meeting);
                code.setMember(member);
                code.setCode(generateUniqueVotingCode());

                meetingVotingCodeRepository.save(code);

                member.setMeetingVotingCode(code.getCode());

            } else {

                member.setMeetingVotingCode(existing.get().getCode());
            }
        }
    }

    private void ensureAttendanceExistsForAllMembers(Meeting meeting) {
        for (Member member : memberRepository.findAll()) {
            Optional<MeetingAttendance> existing = meetingAttendanceRepository.findByMeetingAndMember(meeting, member);

            if (existing.isEmpty()) {
                MeetingAttendance attendance = new MeetingAttendance();
                attendance.setMeeting(meeting);
                attendance.setMember(member);
                attendance.setPresent(true);
                meetingAttendanceRepository.save(attendance);
            }
        }
    }

    private List<Long> getPresentMemberIds(Meeting meeting) {
        return meetingAttendanceRepository.findByMeeting(meeting)
                .stream()
                .filter(MeetingAttendance::isPresent)
                .map(attendance -> attendance.getMember().getId())
                .toList();
    }

    private void removeMemberAbstentionsForMeeting(Member member, Meeting meeting) {
        List<VotingRound> rounds = votingRoundRepository.findByMeeting(meeting);

        for (VotingRound round : rounds) {
            Optional<VoteAbstention> existing = voteAbstentionRepository.findByVotingRoundAndMember(round, member);

            existing.ifPresent(voteAbstentionRepository::delete);
        }
    }

    private Map<Long, List<Long>> getAbstainingMemberIdsByRound(List<VotingRound> rounds) {
        Map<Long, List<Long>> map = new HashMap<>();

        for (VotingRound round : rounds) {
            List<Long> ids = voteAbstentionRepository.findByVotingRound(round)
                    .stream()
                    .map(abstention -> abstention.getMember().getId())
                    .toList();

            map.put(round.getId(), ids);
        }

        return map;
    }

    private Map<Long, Map<Long, String>> buildMemberRoundStatusMap(
            Meeting meeting,
            List<Member> ordinaryMembers,
            List<Member> deputyMembers,
            List<Member> allMembers,
            List<VotingRound> rounds) {
        Map<Long, Map<Long, String>> statusMap = new HashMap<>();

        List<MeetingAttendance> attendanceList = meetingAttendanceRepository.findByMeeting(meeting);

        Set<Long> presentMemberIds = new HashSet<>();
        for (MeetingAttendance attendance : attendanceList) {
            if (attendance.isPresent()) {
                presentMemberIds.add(attendance.getMember().getId());
            }
        }

        for (Member member : allMembers) {
            statusMap.put(member.getId(), new HashMap<>());
        }

        for (VotingRound round : rounds) {
            List<VoteAbstention> abstentions = voteAbstentionRepository.findByVotingRound(round);

            Set<Long> abstainingMemberIds = new HashSet<>();
            for (VoteAbstention abstention : abstentions) {
                abstainingMemberIds.add(abstention.getMember().getId());
            }

            List<Member> eligibleVoters = votingService.getEligibleVotersForRound(
                    ordinaryMembers,
                    deputyMembers,
                    attendanceList,
                    abstentions);

            Set<Long> eligibleMemberIds = new HashSet<>();
            for (Member eligible : eligibleVoters) {
                eligibleMemberIds.add(eligible.getId());
            }

            for (Member member : allMembers) {
                String status;

                if (!presentMemberIds.contains(member.getId())) {
                    status = "Absent";
                } else if (abstainingMemberIds.contains(member.getId())) {
                    status = "Abstain";
                } else if (eligibleMemberIds.contains(member.getId())) {
                    status = "Voting";
                } else {
                    status = "Not Voting";
                }
                statusMap.get(member.getId()).put(round.getId(), status);
            }
        }

        return statusMap;
    }

    private Map<Long, Map<Long, String>> buildMemberRoundStatusClassMap(
            Map<Long, Map<Long, String>> statusMap) {

        Map<Long, Map<Long, String>> classMap = new HashMap<>();

        for (Map.Entry<Long, Map<Long, String>> memberEntry : statusMap.entrySet()) {
            Map<Long, String> roundClassMap = new HashMap<>();

            for (Map.Entry<Long, String> roundEntry : memberEntry.getValue().entrySet()) {
                String status = roundEntry.getValue();

                String cssClass = switch (status) {
                    case "Voting" -> "matrix-eligible";
                    case "Abstain" -> "matrix-abstain";
                    case "Not Voting" -> "matrix-locked";
                    case "Absent" -> "matrix-absent";
                    default -> "matrix-locked";
                };
                roundClassMap.put(roundEntry.getKey(), cssClass);
            }

            classMap.put(memberEntry.getKey(), roundClassMap);
        }

        return classMap;
    }

    private Map<Long, Integer> buildEligibleVotesByRound(
            Meeting meeting,
            List<Member> ordinaryMembers,
            List<Member> deputyMembers,
            List<VotingRound> rounds) {
        Map<Long, Integer> map = new HashMap<>();
        List<MeetingAttendance> attendanceList = meetingAttendanceRepository.findByMeeting(meeting);

        for (VotingRound round : rounds) {
            List<VoteAbstention> abstentions = voteAbstentionRepository.findByVotingRound(round);

            int eligibleVotes = votingService.getEligibleVotersForRound(
                    ordinaryMembers,
                    deputyMembers,
                    attendanceList,
                    abstentions).size();

            map.put(round.getId(), eligibleVotes);
        }

        return map;
    }

    private Map<Long, Boolean> buildQuorumReachedByRound(Map<Long, Integer> eligibleVotesByRound) {
        Map<Long, Boolean> map = new HashMap<>();

        for (Map.Entry<Long, Integer> entry : eligibleVotesByRound.entrySet()) {
            map.put(entry.getKey(), entry.getValue() >= 11);
        }

        return map;
    }

    private int countStatus(Map<Long, Map<Long, String>> statusMap, String statusToCount) {
        int count = 0;

        for (Map<Long, String> roundStatuses : statusMap.values()) {
            for (String status : roundStatuses.values()) {
                if (statusToCount.equals(status)) {
                    count++;
                }
            }
        }

        return count;
    }

    @GetMapping
    public String admin(Model model) {

        List<Meeting> meetings = meetingRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Meeting::getDate).reversed())
                .toList();

        model.addAttribute("meetings", meetings);

        return "admin";
    }

    @PostMapping("/meetings")
    public String createMeeting(@RequestParam String title, @RequestParam String date) {
        Meeting meeting = new Meeting();
        meeting.setTitle(title);
        meeting.setDate(LocalDate.parse(date));
        meetingRepository.save(meeting);

        return "redirect:/admin";
    }

    @GetMapping("/members")
    public String members(Model model) {
        List<Member> ordinaryMembers = memberRepository.findByRoleOrderByIdAsc("ORDINARY");
        List<Member> deputyMembers = memberRepository.findByRoleOrderByIdAsc("DEPUTY");

        model.addAttribute("ordinaryMembers", ordinaryMembers);
        model.addAttribute("deputyMembers", deputyMembers);
        model.addAttribute("ordinaryCount", ordinaryMembers.size());
        model.addAttribute("deputyCount", deputyMembers.size());
        model.addAttribute("ordinaryFull", ordinaryMembers.size() >= 21);
        model.addAttribute("deputyFull", deputyMembers.size() >= 10);
        model.addAttribute("allPositionsFull", ordinaryMembers.size() >= 21 && deputyMembers.size() >= 10);

        return "members";
    }

    ArrayList<String> generateNamesOrdinary = new ArrayList<>(Arrays.asList(
            "Alena Cicek",
            "Andrea Romanus",
            "Arvid Marklund",
            "Axel Jonovski",
            "Daniel Stjernkvist",
            "Elina Entezari",
            "Eoin Dunne",
            "Gaia Zubrickaite",
            "Hampus Krook",
            "Hanna Månendahl",
            "Hugo Jeppsson",
            "John Klintell",
            "Kajsa Brunsten",
            "Kajsa Åberg",
            "Linn Gellar",
            "Mauritz Christensson",
            "Olof Lagerwall",
            "Ronja Gerdau",
            "Tilly Lindhé Nilsson",
            "Victor Forsberg Trägårdh",
            "Wilhelm Siwers"));

    ArrayList<String> generateNamesDeputy = new ArrayList<>(Arrays.asList(
            "Linus Strandwitz",
            "Filip Biderholt",
            "Noam Arad",
            "Ella Rogberg",
            "Isabel Mc Conell",
            "Elliot Orrelid",
            "Edit Bernsfelt",
            "Olivia Olsson",
            "Elias Anderson",
            "Boaz van der Schaaf"));

    @PostMapping("/members/default")
    public String createDefaultMembers() {

        if (!memberRepository.findAll().isEmpty()) {
            return "redirect:/admin/members";
        }

        for (int i = 1; i <= 21; i++) {

            Member member = new Member();

            member.setName(generateNamesOrdinary.get(i - 1));
            member.setRole("ORDINARY");

            memberRepository.save(member);
        }

        for (int i = 1; i <= 10; i++) {

            Member member = new Member();

            member.setName(generateNamesDeputy.get(i - 1));
            member.setRole("DEPUTY");

            memberRepository.save(member);
        }

        return "redirect:/admin/members";
    }

    @PostMapping("/members")
    public String addMember(
            @RequestParam String name,
            @RequestParam String role) {

        long ordinaryCount = memberRepository.findByRole("ORDINARY").size();

        long deputyCount = memberRepository.findByRole("DEPUTY").size();

        if ("ORDINARY".equals(role) && ordinaryCount >= 21) {
            return "redirect:/admin/members";
        }

        if ("DEPUTY".equals(role) && deputyCount >= 10) {
            return "redirect:/admin/members";
        }

        Member member = new Member();

        member.setName(name);
        member.setRole(role);

        memberRepository.save(member);

        return "redirect:/admin/members";
    }

    @PostMapping("/members/save-all")
    public String saveAllMembers(
            @RequestParam(required = false) List<Long> memberIds,
            @RequestParam(required = false) List<String> names,
            @RequestParam(required = false) List<String> roles) {

        if (memberIds == null || names == null || roles == null) {
            return "redirect:/admin/members";
        }

        for (int i = 0; i < memberIds.size(); i++) {
            Member member = memberRepository.findById(memberIds.get(i)).orElseThrow();

            String newName = names.get(i);
            String newRole = roles.get(i);

            if (!member.getRole().equals(newRole)) {
                long ordinaryCount = memberRepository.findByRole("ORDINARY").size();
                long deputyCount = memberRepository.findByRole("DEPUTY").size();

                if ("ORDINARY".equals(newRole) && ordinaryCount >= 21) {
                    continue;
                }

                if ("DEPUTY".equals(newRole) && deputyCount >= 10) {
                    continue;
                }
            }

            member.setName(newName);
            member.setRole(newRole);

            memberRepository.save(member);
        }

        return "redirect:/admin/members";
    }

    @PostMapping("/members/{id}/delete")
    public String deleteMember(@PathVariable Long id) {
        Member member = memberRepository.findById(id).orElseThrow();

        for (MeetingAttendance attendance : meetingAttendanceRepository.findAll()) {
            if (attendance.getMember().getId().equals(member.getId())) {
                meetingAttendanceRepository.delete(attendance);
            }
        }

        for (VoteAbstention abstention : voteAbstentionRepository.findAll()) {
            if (abstention.getMember().getId().equals(member.getId())) {
                voteAbstentionRepository.delete(abstention);
            }
        }

        for (MeetingVotingCode code : meetingVotingCodeRepository.findAll()) {
            if (code.getMember().getId().equals(member.getId())) {
                meetingVotingCodeRepository.delete(code);
            }
        }

        memberRepository.delete(member);

        return "redirect:/admin/members";
    }

    @GetMapping("/meetings/{id}")
    public String meeting(@PathVariable Long id, Model model) {

        Meeting meeting = meetingRepository.findById(id).orElseThrow();

        ensureAttendanceExistsForAllMembers(meeting);
        ensureMeetingVotingCodesExist(meeting);

        List<Member> ordinaryMembers = memberRepository.findByRoleOrderByIdAsc("ORDINARY");
        List<Member> deputyMembers = memberRepository.findByRoleOrderByIdAsc("DEPUTY");

        List<Member> allMembers = new ArrayList<>();
        allMembers.addAll(ordinaryMembers);
        allMembers.addAll(deputyMembers);

        for (MeetingVotingCode code : meetingVotingCodeRepository.findByMeeting(meeting)) {
            if (code.getMember() != null) {
                for (Member member : allMembers) {
                    if (member.getId().equals(code.getMember().getId())) {
                        member.setMeetingVotingCode(code.getCode());
                    }
                }
            }
        }

        List<VotingRound> rounds = votingRoundRepository.findByMeetingOrderByDisplayOrderAscIdAsc(meeting);
        List<Long> presentMemberIds = getPresentMemberIds(meeting);

        Map<Long, Map<Long, String>> memberRoundStatusMap = buildMemberRoundStatusMap(meeting, ordinaryMembers,
                deputyMembers, allMembers, rounds);

        Map<Long, Map<Long, String>> memberRoundStatusClassMap = buildMemberRoundStatusClassMap(memberRoundStatusMap);

        Map<Long, Integer> eligibleVotesByRound = buildEligibleVotesByRound(meeting, ordinaryMembers, deputyMembers,
                rounds);

        Map<Long, Boolean> quorumReachedByRound = buildQuorumReachedByRound(eligibleVotesByRound);

        model.addAttribute("meeting", meeting);
        model.addAttribute("ordinaryMembers", ordinaryMembers);
        model.addAttribute("deputyMembers", deputyMembers);
        model.addAttribute("allMembers", allMembers);
        model.addAttribute("presentMemberIds", presentMemberIds);
        model.addAttribute("presentCount", presentMemberIds.size());
        model.addAttribute("totalMemberCount", allMembers.size());
        model.addAttribute("rounds", rounds);
        model.addAttribute("roundCount", rounds.size());
        model.addAttribute("memberRoundStatusMap", memberRoundStatusMap);
        model.addAttribute("memberRoundStatusClassMap", memberRoundStatusClassMap);
        model.addAttribute("eligibleVotesByRound", eligibleVotesByRound);
        model.addAttribute("quorumReachedByRound", quorumReachedByRound);
        model.addAttribute("totalAbstentions", countStatus(memberRoundStatusMap, "Abstain"));

        return "meeting";
    }

    @GetMapping("/rounds/{id}/edit")
    public String editVotingRound(
            @PathVariable Long id,
            @RequestParam(required = false) String publishError,
            Model model) {

        VotingRound round = votingRoundRepository.findById(id).orElseThrow();
        Meeting meeting = round.getMeeting();

        ensureAttendanceExistsForAllMembers(meeting);
        ensureMeetingVotingCodesExist(meeting);

        List<Member> ordinaryMembers = memberRepository.findByRoleOrderByIdAsc("ORDINARY");
        List<Member> deputyMembers = memberRepository.findByRoleOrderByIdAsc("DEPUTY");

        List<Member> allMembers = new ArrayList<>();
        allMembers.addAll(ordinaryMembers);
        allMembers.addAll(deputyMembers);

        for (MeetingVotingCode code : meetingVotingCodeRepository.findByMeeting(meeting)) {
            if (code.getMember() != null) {
                for (Member member : allMembers) {
                    if (member.getId().equals(code.getMember().getId())) {
                        member.setMeetingVotingCode(code.getCode());
                    }
                }
            }
        }

        List<MeetingAttendance> attendanceList = meetingAttendanceRepository.findByMeeting(meeting);
        List<VoteAbstention> abstentions = voteAbstentionRepository.findByVotingRound(round);
        List<Alternative> alternatives = alternativeRepository.findByVotingRound(round);
        List<Vote> votes = voteRepository.findByVotingRound(round);

        Map<String, Object> results = votingService.calculateResults(
                round,
                ordinaryMembers,
                deputyMembers,
                attendanceList,
                alternatives,
                votes,
                abstentions);

        model.addAttribute("round", round);
        model.addAttribute("meeting", meeting);
        model.addAttribute("alternatives", alternatives);
        model.addAttribute("results", results);
        model.addAttribute("publishError", "nominationRequired".equals(publishError));

        return "round-edit";
    }

    @PostMapping("/meetings/{meetingId}/members/{memberId}/toggle-present")
    public String togglePresent(@PathVariable Long meetingId, @PathVariable Long memberId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElseThrow();
        Member member = memberRepository.findById(memberId).orElseThrow();

        Optional<MeetingAttendance> existing = meetingAttendanceRepository.findByMeetingAndMember(meeting, member);

        MeetingAttendance attendance;

        if (existing.isPresent()) {
            attendance = existing.get();
            attendance.setPresent(!attendance.isPresent());
        } else {
            attendance = new MeetingAttendance();
            attendance.setMeeting(meeting);
            attendance.setMember(member);
            attendance.setPresent(false);
        }

        meetingAttendanceRepository.save(attendance);

        if (!attendance.isPresent()) {
            removeMemberAbstentionsForMeeting(member, meeting);
        }

        return "redirect:/admin/meetings/" + meetingId;
    }

    @PostMapping("/meetings/{meetingId}/rounds/{roundId}/members/{memberId}/toggle-abstain")
    public String toggleVoteAbstentionFromMatrix(
            @PathVariable Long meetingId,
            @PathVariable Long roundId,
            @PathVariable Long memberId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElseThrow();
        VotingRound round = votingRoundRepository.findById(roundId).orElseThrow();
        Member member = memberRepository.findById(memberId).orElseThrow();

        if (!round.getMeeting().getId().equals(meeting.getId())) {
            return "redirect:/admin/meetings/" + meetingId;
        }

        if (round.isPublished()) {
            return "redirect:/admin/meetings/" + meetingId;
        }

        Optional<MeetingAttendance> attendance = meetingAttendanceRepository.findByMeetingAndMember(meeting, member);

        if (attendance.isEmpty() || !attendance.get().isPresent()) {
            return "redirect:/admin/meetings/" + meetingId;
        }

        Optional<VoteAbstention> existing = voteAbstentionRepository.findByVotingRoundAndMember(round, member);

        if (existing.isPresent()) {
            voteAbstentionRepository.delete(existing.get());
        } else {
            VoteAbstention abstention = new VoteAbstention();
            abstention.setVotingRound(round);
            abstention.setMember(member);
            voteAbstentionRepository.save(abstention);
        }

        return "redirect:/admin/meetings/" + meetingId;
    }

    @PostMapping("/meetings/{id}/rounds")
    public String createVotingRound(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam int maxSelections,
            @RequestParam String majorityRule,
            @RequestParam(required = false) String includeBlank,
            @RequestParam(required = false) String includeVacant) {

        Meeting meeting = meetingRepository.findById(id).orElseThrow();

        int nextOrder = votingRoundRepository.findByMeeting(meeting).size() + 1;

        VotingRound round = new VotingRound();
        round.setTitle(title);
        round.setMaxSelections(maxSelections);
        round.setMajorityRule(majorityRule);
        round.setIncludeBlank(includeBlank != null);
        round.setIncludeVacant(includeVacant != null);
        round.setDisplayOrder(nextOrder);
        round.setPublished(false);
        round.setClosed(false);
        round.setMeeting(meeting);

        votingRoundRepository.save(round);

        return "redirect:/admin/meetings/" + id;
    }

    @PostMapping("/meetings/{id}/rounds/reorder")
    @ResponseBody
    public String reorderVotingRounds(
            @PathVariable Long id,
            @RequestParam String roundIds) {
        Meeting meeting = meetingRepository.findById(id).orElseThrow();

        String[] ids = roundIds.split(",");

        for (int i = 0; i < ids.length; i++) {
            Long roundId = Long.parseLong(ids[i]);
            VotingRound round = votingRoundRepository.findById(roundId).orElseThrow();

            if (round.getMeeting().getId().equals(meeting.getId())) {
                round.setDisplayOrder(i + 1);
                votingRoundRepository.save(round);
            }
        }

        return "OK";
    }

    @PostMapping("/rounds/{id}/update")
    public String updateVotingRound(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam int maxSelections,
            @RequestParam String majorityRule,
            @RequestParam(required = false) String includeBlank,
            @RequestParam(required = false) String includeVacant) {
        VotingRound round = votingRoundRepository.findById(id).orElseThrow();

        if (!round.isPublished()) {
            round.setTitle(title);
            round.setMaxSelections(maxSelections);
            round.setMajorityRule(majorityRule);
            round.setIncludeBlank(includeBlank != null);
            round.setIncludeVacant(includeVacant != null);
            votingRoundRepository.save(round);
        }

        return "redirect:/admin/rounds/" + round.getId() + "/edit";
    }

    @PostMapping("/rounds/{id}/delete")
    public String deleteVotingRound(@PathVariable Long id) {
        VotingRound round = votingRoundRepository.findById(id).orElseThrow();
        Long meetingId = round.getMeeting().getId();

        for (Vote vote : voteRepository.findByVotingRound(round)) {
            voteRepository.delete(vote);
        }

        for (VoteAbstention abstention : voteAbstentionRepository.findByVotingRound(round)) {
            voteAbstentionRepository.delete(abstention);
        }

        for (Alternative alternative : alternativeRepository.findByVotingRound(round)) {
            alternativeRepository.delete(alternative);
        }

        votingRoundRepository.delete(round);

        return "redirect:/admin/meetings/" + meetingId;
    }

    @PostMapping("/rounds/{id}/publish")
    public String publishVotingRound(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "true") boolean nominationRequired) {

        VotingRound round = votingRoundRepository.findById(id).orElseThrow();

        if (nominationRequired) {
            boolean hasNominatedCandidate = alternativeRepository.findByVotingRound(round)
                    .stream()
                    .anyMatch(Alternative::isNominated);

            if (!hasNominatedCandidate) {
                return "redirect:/admin/rounds/" + round.getId() + "/edit?publishError=nominationRequired";
            }
        }

        round.setPublished(true);
        round.setClosed(false);
        votingRoundRepository.save(round);

        return "redirect:/admin/rounds/" + round.getId() + "/edit";
    }

    @PostMapping("/rounds/{id}/alternatives")
    public String addAlternative(@PathVariable Long id, @RequestParam String text) {

        VotingRound round = votingRoundRepository.findById(id).orElseThrow();

        if (round.isPublished()) {
            return "redirect:/admin/rounds/" + round.getId() + "/edit";
        }

        List<Alternative> existingAlternatives = alternativeRepository.findByVotingRound(round);

        boolean shouldAutoNominate = existingAlternatives.isEmpty();

        Alternative alternative = new Alternative();
        alternative.setText(text);
        alternative.setNominated(shouldAutoNominate);
        alternative.setVotingRound(round);

        alternativeRepository.save(alternative);

        return "redirect:/admin/rounds/" + round.getId() + "/edit";
    }

    @PostMapping("/rounds/{id}/alternatives/save-all")
    public String saveAllAlternatives(
            @PathVariable Long id,
            @RequestParam(required = false) List<Long> alternativeIds,
            @RequestParam(required = false) List<String> alternativeTexts,
            @RequestParam(required = false) Long nominatedAlternativeId) {
        VotingRound round = votingRoundRepository.findById(id).orElseThrow();

        if (round.isPublished()) {
            return "redirect:/admin/rounds/" + round.getId() + "/edit";
        }

        for (Alternative alternative : alternativeRepository.findByVotingRound(round)) {
            alternative.setNominated(false);
            alternativeRepository.save(alternative);
        }

        if (alternativeIds == null || alternativeTexts == null) {
            return "redirect:/admin/rounds/" + round.getId() + "/edit";
        }

        for (int i = 0; i < alternativeIds.size(); i++) {
            Alternative alternative = alternativeRepository.findById(alternativeIds.get(i)).orElseThrow();

            if (!alternative.getVotingRound().getId().equals(round.getId())) {
                continue;
            }

            alternative.setText(alternativeTexts.get(i));
            alternative.setNominated(
                    nominatedAlternativeId != null
                            && nominatedAlternativeId > 0
                            && alternative.getId().equals(nominatedAlternativeId));

            alternativeRepository.save(alternative);
        }

        return "redirect:/admin/rounds/" + round.getId() + "/edit";
    }

    @PostMapping("/alternatives/{id}/delete")
    public String deleteAlternative(@PathVariable Long id) {
        Alternative alternative = alternativeRepository.findById(id).orElseThrow();
        VotingRound round = alternative.getVotingRound();

        if (!round.isPublished()) {
            alternativeRepository.delete(alternative);
        }

        return "redirect:/admin/rounds/" + round.getId() + "/edit";
    }

    @PostMapping("/rounds/{roundId}/members/{memberId}/toggle-abstain")
    public String toggleVoteAbstention(
            @PathVariable Long roundId,
            @PathVariable Long memberId) {
        VotingRound round = votingRoundRepository.findById(roundId).orElseThrow();

        if (round.isPublished()) {
            return "redirect:/admin/rounds/" + round.getId() + "/edit";
        }

        Member member = memberRepository.findById(memberId).orElseThrow();

        Optional<MeetingAttendance> attendance = meetingAttendanceRepository.findByMeetingAndMember(round.getMeeting(),
                member);

        if (attendance.isEmpty() || !attendance.get().isPresent()) {
            return "redirect:/admin/rounds/" + round.getId() + "/edit";
        }

        Optional<VoteAbstention> existing = voteAbstentionRepository.findByVotingRoundAndMember(round, member);

        if (existing.isPresent()) {
            voteAbstentionRepository.delete(existing.get());
        } else {
            VoteAbstention abstention = new VoteAbstention();
            abstention.setVotingRound(round);
            abstention.setMember(member);
            voteAbstentionRepository.save(abstention);
        }

        return "redirect:/admin/rounds/" + round.getId() + "/edit";
    }

    @PostMapping("/rounds/{id}/close")
    public String closeVotingRound(@PathVariable Long id) {

        VotingRound round = votingRoundRepository.findById(id).orElseThrow();

        if (round.isPublished()) {
            round.setClosed(true);
            votingRoundRepository.save(round);
        }

        return "redirect:/admin/rounds/" + round.getId() + "/edit";
    }
}