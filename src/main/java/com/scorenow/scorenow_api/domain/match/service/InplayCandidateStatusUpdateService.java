package com.scorenow.scorenow_api.domain.match.service;

import com.scorenow.scorenow_api.domain.match.dto.MatchCandidate;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.repository.jpa.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.scorenow.scorenow_api.domain.match.entity.MatchStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InplayCandidateStatusUpdateService {

    private final MatchRepository matchRepository;

    @Transactional
    public int updateInplayStatuses(List<MatchCandidate> candidates) {
        return transitionStatus(candidates, IN_PLAY);
    }

    @Transactional
    public int updateToBeFixedStatuses(List<MatchCandidate> candidates) {
        return transitionStatus(candidates, TO_BE_FIXED);
    }

    private int transitionStatus(List<MatchCandidate> candidates, MatchStatus matchStatus) {
        if (candidates.isEmpty()) {
            return 0;
        }

        List<Long> matchIds = candidates.stream()
                .map(MatchCandidate::getMatchId)
                .toList();

        return matchRepository.updateStatusBulk(matchIds, matchStatus);
    }


    @Transactional
    public void updateMatchStatuses(List<MatchCandidate> inplayMatches, List<MatchCandidate> toBeFixedMatches) {
        if (!inplayMatches.isEmpty()) {
            List<Long> matchIds = inplayMatches.stream()
                    .map(MatchCandidate::getMatchId)
                    .toList();

            int updatedCount = matchRepository.updateStatusBulk(matchIds, IN_PLAY);
        }

        if (!toBeFixedMatches.isEmpty()) {
            List<Long> matchIds = toBeFixedMatches.stream()
                    .map(MatchCandidate::getMatchId)
                    .toList();

            matchRepository.updateStatusBulk(matchIds, TO_BE_FIXED);
        }
    }

}
