package com.example.votingsystem.service;

import com.example.votingsystem.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class VotingService {

    public List<Member> getEligibleVotersForRound(
            List<Member> ordinaryMembers,
            List<Member> deputyMembers,
            List<MeetingAttendance> attendanceList,
            List<VoteAbstention> abstentions
    ) {
        List<Member> eligibleVoters = new ArrayList<>();

        Set<Long> presentMemberIds = new HashSet<>();
        for (MeetingAttendance attendance : attendanceList) {
            if (attendance.isPresent()) {
                presentMemberIds.add(attendance.getMember().getId());
            }
        }

        Set<Long> abstainingMemberIds = new HashSet<>();
        for (VoteAbstention abstention : abstentions) {
            abstainingMemberIds.add(abstention.getMember().getId());
        }

        int deputyIndex = 0;

        for (Member ordinary : ordinaryMembers) {
            boolean ordinaryPresent = presentMemberIds.contains(ordinary.getId());
            boolean ordinaryAbstaining = abstainingMemberIds.contains(ordinary.getId());

            if (ordinaryPresent && !ordinaryAbstaining) {
                eligibleVoters.add(ordinary);
            } else {
                while (deputyIndex < deputyMembers.size()) {
                    Member deputy = deputyMembers.get(deputyIndex);
                    deputyIndex++;

                    boolean deputyPresent = presentMemberIds.contains(deputy.getId());
                    boolean deputyAbstaining = abstainingMemberIds.contains(deputy.getId());

                    if (deputyPresent && !deputyAbstaining) {
                        eligibleVoters.add(deputy);
                        break;
                    }
                }
            }
        }

        return eligibleVoters;
    }

    public Map<String, Object> calculateResults(
            VotingRound round,
            List<Member> ordinaryMembers,
            List<Member> deputyMembers,
            List<MeetingAttendance> attendanceList,
            List<Alternative> alternatives,
            List<Vote> votes,
            List<VoteAbstention> abstentions
    ) {
        Map<String, Object> result = new HashMap<>();

        List<Member> eligibleVoters = getEligibleVotersForRound(
                ordinaryMembers,
                deputyMembers,
                attendanceList,
                abstentions
        );

        int eligibleVotes = eligibleVoters.size();
        boolean quorumReached = eligibleVotes >= 11;

        int abstentionCount = abstentions.size();

        int blankVotes = 0;
        int vacantVotes = 0;

        for (Vote vote : votes) {
            if ("BLANK".equals(vote.getVoteType())) {
                blankVotes++;
            }

            if ("VACANT".equals(vote.getVoteType())) {
                vacantVotes++;
            }
        }

        int votesCast = votes.size();
        int decisionVotes = votesCast - blankVotes;

        int notVoted = eligibleVotes - votesCast;
        if (notVoted < 0) {
            notVoted = 0;
        }

        boolean allExpectedVotesCast = notVoted == 0;

        int countedVotesForMajority = eligibleVotes - blankVotes;
        if (countedVotesForMajority < 0) {
            countedVotesForMajority = 0;
        }

        Map<String, Integer> alternativeResults = new LinkedHashMap<>();
        List<Map<String, Object>> alternativeRows = new ArrayList<>();

        for (Alternative alternative : alternatives) {
            int count = 0;

            for (Vote vote : votes) {
                if ("NORMAL".equals(vote.getVoteType())
                        && vote.getSelectedAlternatives().contains(alternative)) {
                    count++;
                }
            }

            alternativeResults.put(alternative.getText(), count);

            Map<String, Object> row = new HashMap<>();
            row.put("id", alternative.getId());
            row.put("text", alternative.getText());
            row.put("nominated", alternative.isNominated());
            row.put("votes", count);
            alternativeRows.add(row);
        }

        if (round.isIncludeBlank()) {
            Map<String, Object> blankRow = new HashMap<>();
            blankRow.put("id", -1L);
            blankRow.put("text", "Blank");
            blankRow.put("nominated", false);
            blankRow.put("votes", blankVotes);

            alternativeRows.add(blankRow);
            alternativeResults.put("Blank", blankVotes);
        }

        if (round.isIncludeVacant()) {
            Map<String, Object> vacantRow = new HashMap<>();
            vacantRow.put("id", 0L);
            vacantRow.put("text", "Vacant");
            vacantRow.put("nominated", false);
            vacantRow.put("votes", vacantVotes);

            alternativeRows.add(vacantRow);
            alternativeResults.put("Vacant", vacantVotes);
        }

        int highestVotes = 0;

        for (Integer count : alternativeResults.values()) {
            if (count > highestVotes) {
                highestVotes = count;
            }
        }

        List<String> leadingNames = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : alternativeResults.entrySet()) {
            if (entry.getValue() == highestVotes && highestVotes > 0) {
                leadingNames.add(entry.getKey());
            }
        }

        boolean tiedLead = leadingNames.size() > 1;

        Alternative nominatedAlternative = null;

        for (Alternative alternative : alternatives) {
            if (alternative.isNominated()) {
                nominatedAlternative = alternative;
                break;
            }
        }

        boolean tieResolvedByNomination = false;
        String leadingAlternativeText;

        if (leadingNames.isEmpty()) {
            leadingAlternativeText = "No votes yet";
        } else if (tiedLead && nominatedAlternative != null && leadingNames.contains(nominatedAlternative.getText())) {
            leadingAlternativeText = nominatedAlternative.getText() + " (by Nomination)";
            tieResolvedByNomination = true;
        } else if (tiedLead) {
            leadingAlternativeText = "Tie: " + String.join(", ", leadingNames);
        } else {
            leadingAlternativeText = leadingNames.get(0);
        }

        int requiredVotes = calculateRequiredVotes(countedVotesForMajority, round.getMajorityRule());

        boolean unresolvedTie = tiedLead && !tieResolvedByNomination;

        boolean passed =
                quorumReached
                        && !unresolvedTie
                        && highestVotes >= requiredVotes;

        result.put("eligibleVotes", eligibleVotes);
        result.put("countedVotesForMajority", countedVotesForMajority);
        result.put("quorumReached", quorumReached);
        result.put("votesCast", votesCast);
        result.put("decisionVotes", decisionVotes);
        result.put("blankVotes", blankVotes);
        result.put("vacantVotes", vacantVotes);
        result.put("abstentions", abstentionCount);
        result.put("notVoted", notVoted);
        result.put("allExpectedVotesCast", allExpectedVotesCast);

        result.put("alternativeResults", alternativeResults);
        result.put("alternativeRows", alternativeRows);

        result.put("eligibleVoters", eligibleVoters);

        result.put("leadingAlternativeText", leadingAlternativeText);
        result.put("highestVotes", highestVotes);

        result.put("tiedLead", tiedLead);
        result.put("tieResolvedByNomination", tieResolvedByNomination);
        result.put("unresolvedTie", unresolvedTie);

        result.put("requiredVotes", requiredVotes);
        result.put("passed", passed);
        result.put("majorityRule", round.getMajorityRule());

        return result;
    }

    private int calculateRequiredVotes(int baseVotes, String majorityRule) {
        if (baseVotes <= 0) {
            return 0;
        }

        if ("TWO_THIRDS".equals(majorityRule)) {
            return (int) Math.ceil(baseVotes * (2.0 / 3.0));
        }

        if ("FOUR_FIFTHS".equals(majorityRule)) {
            return (int) Math.ceil(baseVotes * (4.0 / 5.0));
        }

        return (baseVotes / 2) + 1;
    }
}